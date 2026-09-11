/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.PriorityQueue;

import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.Utils;

/*************************************************************************************************/
/********************************* Schedules tasks within a plan *********************************/
/*************************************************************************************************/

public class Schedule
{
  private static final Comparator<Node> NODE_ORDER = Comparator.comparingLong( node -> node.m_orderKey );

  private static class Node
  {
    private final Task      m_task;
    private final int       m_index;
    private final long      m_orderKey;
    private int             m_incoming;
    private ArrayList<Node> m_successors;

    private Node( Task task, int index )
    {
      m_task = task;
      m_index = index;
      m_orderKey = ( (long) ( 999 - task.getPriority() ) << 32 ) | index;
    }

    private void addSuccessor( Node successor )
    {
      // allocate successor storage only for tasks that need it
      if ( m_successors == null )
        m_successors = new ArrayList<>( 2 );

      m_successors.add( successor );
    }
  }

  /****************************************** schedule *******************************************/
  public static void schedule( Plan plan )
  {
    // a simple serial scheduler
    Utils.trace( "~~~~~~~~~~~~~~~~ SCHEDULE ~~~~~~~~~~~~~~~~" );
    var list = getOrderedTasks( plan.getTasks() );

    Utils.trace( list );
  }

  /*************************************** getOrderedTasks ***************************************/
  private static List<Task> getOrderedTasks( Tasks tasks )
  {
    // create scheduling nodes for non-blank tasks, excluding the overall-project task
    int taskCount = tasks.getNotNullCount();
    var nodes = new Node[tasks.size()];
    var nodesByTask = new IdentityHashMap<Task, Node>( taskCount );

    for ( int index = 1; index < tasks.size(); index++ )
    {
      Task task = tasks.get( index );
      if ( !task.isBlank() )
      {
        Node node = new Node( task, index );
        nodes[index] = node;
        nodesByTask.put( task, node );
      }
    }

    // build incoming counts and successor links from predecessor relationships
    for ( Node node : nodes )
    {
      if ( node == null )
        continue;

      var predecessors = node.m_task.getPredecessors();
      if ( predecessors == null )
        continue;

      for ( int index = 0; index < predecessors.size(); index++ )
      {
        Node predecessor = nodesByTask.get( predecessors.get( index ).task );
        if ( predecessor == null )
          throw new IllegalStateException( "Invalid predecessor for task " + node.m_index );

        node.m_incoming++;
        predecessor.addSuccessor( node );
      }
    }

    // queue eligible tasks by descending priority and then ascending task index
    var ready = new PriorityQueue<Node>( Math.max( 1, taskCount ), NODE_ORDER );
    for ( Node node : nodes )
      if ( node != null && node.m_incoming == 0 )
        ready.add( node );

    // remove each best eligible task and release its successors
    var ordered = new ArrayList<Task>( taskCount );
    while ( !ready.isEmpty() )
    {
      Node node = ready.remove();
      ordered.add( node.m_task );

      if ( node.m_successors != null )
        for ( Node successor : node.m_successors )
          if ( --successor.m_incoming == 0 )
            ready.add( successor );
    }

    // any unprocessed tasks form a circular dependency
    if ( ordered.size() != taskCount )
      throw new IllegalStateException( "Circular task dependency" );

    return ordered;
  }

}
