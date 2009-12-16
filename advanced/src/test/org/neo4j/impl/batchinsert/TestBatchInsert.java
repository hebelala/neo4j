/*
 * Copyright (c) 2002-2009 "Neo Technology,"
 *     Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 * 
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.impl.batchinsert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.impl.AbstractNeoTestCase;

public class TestBatchInsert extends TestCase
{
    private static Map<String,Object> properties = new HashMap<String,Object>();

    private static enum RelTypes implements RelationshipType
    {
        BATCH_TEST,
        REL_TYPE1,
        REL_TYPE2,
        REL_TYPE3,
        REL_TYPE4,
        REL_TYPE5
    }
    
    private static RelationshipType[] relTypeArray = { 
        RelTypes.REL_TYPE1, RelTypes.REL_TYPE2, RelTypes.REL_TYPE3,
        RelTypes.REL_TYPE4, RelTypes.REL_TYPE5 };
    
    static
    {
        properties.put( "key0", "SDSDASSDLKSDSAKLSLDAKSLKDLSDAKLDSLA" );
        properties.put( "key1", 1 );
        properties.put( "key2", (short) 2 );
        properties.put( "key3", 3L );
        properties.put( "key4", 4.0f );
        properties.put( "key5", 5.0d );
        properties.put( "key6", (byte) 6 );
        properties.put( "key7", true );
        properties.put( "key8", (char) 8 );
        properties.put( "key10", new String[] {
            "SDSDASSDLKSDSAKLSLDAKSLKDLSDAKLDSLA", "dsasda", "dssadsad" 
        } );
        properties.put( "key11", new int[] {1,2,3,4,5,6,7,8,9 } );
        properties.put( "key12", new short[] {1,2,3,4,5,6,7,8,9} );
        properties.put( "key13", new long[] {1,2,3,4,5,6,7,8,9 } );
        properties.put( "key14", new float[] {1,2,3,4,5,6,7,8,9} );
        properties.put( "key15", new double[] {1,2,3,4,5,6,7,8,9} );
        properties.put( "key16", new byte[] {1,2,3,4,5,6,7,8,9} );
        properties.put( "key17", new boolean[] {true,false,true,false} );
        properties.put( "key18", new char[] {1,2,3,4,5,6,7,8,9} );
    }
    
    private BatchInserter newBatchInserter()
    {
        return new BatchInserterImpl(
            AbstractNeoTestCase.getNeoPath( "neo-batch" ) );
    }
    
    public void testSimple()
    {
        BatchInserter neo = newBatchInserter();
        long node1 = neo.createNode( null );
        long node2 = neo.createNode( null );
        long rel1 = neo.createRelationship( node1, node2, RelTypes.BATCH_TEST, 
            null );
        SimpleRelationship rel = neo.getRelationshipById( rel1 );
        assertEquals( rel.getStartNode(), node1 );
        assertEquals( rel.getEndNode(), node2 );
        assertEquals( RelTypes.BATCH_TEST.name(), rel.getType().name() );
        neo.shutdown();
    }
    
    public void testMore()
    {
        BatchInserter neo = newBatchInserter();
        long startNode = neo.createNode( properties );
        long endNodes[] = new long[25];
        Set<Long> rels = new HashSet<Long>();
        for ( int i = 0; i < 25; i++ )
        {
            endNodes[i] = neo.createNode( properties );
            rels.add( neo.createRelationship( startNode, endNodes[i], 
                relTypeArray[i % 5], properties ) );
        }
        for ( SimpleRelationship rel : neo.getRelationships( startNode ) )
        {
            assertTrue( rels.contains( rel.getId() ) );
            assertEquals( rel.getStartNode(), startNode );
        }
        neo.setNodeProperties( startNode, properties );
        neo.shutdown();
    }
    
    private void setProperties( Node node )
    {
        for ( String key : properties.keySet() )
        {
            node.setProperty( key, properties.get( key ) );
        }
    }

    private void setProperties( Relationship rel )
    {
        for ( String key : properties.keySet() )
        {
            rel.setProperty( key, properties.get( key ) );
        }
    }
    
    public void testWithNeoService()
    {
        BatchInserter batchInserter = newBatchInserter();
        NeoService neo = batchInserter.getNeoService();
        Node startNode = neo.createNode();
        setProperties( startNode );
        Node endNodes[] = new Node[25];
        Set<Relationship> rels = new HashSet<Relationship>();
        for ( int i = 0; i < 25; i++ )
        {
            endNodes[i] = neo.createNode();
            setProperties( endNodes[i] );
            Relationship rel = startNode.createRelationshipTo( endNodes[i], 
                relTypeArray[i % 5] ); 
            rels.add( rel );
            setProperties( rel ); 
        }
        for ( Relationship rel : startNode.getRelationships() )
        {
            assertTrue( rels.contains( rel ) );
            assertEquals( rel.getStartNode(), startNode );
        }
        setProperties( startNode );
        neo.shutdown();
    }
    
    public void testNeoServiceGetRelationships()
    {
        BatchInserter batchInserter = newBatchInserter();
        NeoService neo = batchInserter.getNeoService();
        Node startNode = neo.createNode();
        for ( int i = 0; i < 5; i++ )
        {
            Node endNode = neo.createNode();
            startNode.createRelationshipTo( endNode, relTypeArray[i] ); 
        }
        for ( int i = 0; i < 5; i++ )
        {
            assertTrue( startNode.getSingleRelationship( 
                relTypeArray[i], Direction.OUTGOING ) != null );
        }
        for ( int i = 0; i < 5; i++ )
        {
            Iterator<Relationship> relItr = 
                startNode.getRelationships( relTypeArray[i], 
                    Direction.OUTGOING ).iterator();
            relItr.next();
            assertTrue( !relItr.hasNext() );
        }
        for ( int i = 0; i < 5; i++ )
        {
            Iterator<Relationship> relItr = 
                startNode.getRelationships( relTypeArray[i] ).iterator();
            relItr.next();
            assertTrue( !relItr.hasNext() );
        }
        neo.shutdown();
    }
}
