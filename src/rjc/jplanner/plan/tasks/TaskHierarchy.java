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

package rjc.jplanner.plan.tasks;

import java.util.Arrays;
import java.util.List;

/*************************************************************************************************/
/************************ Immutable snapshot of task outline relationships ************************/
/*************************************************************************************************/

public final class TaskHierarchy
{
  private final int[] m_parentIndices; // parent model indexes, -1 for task 0 and blank rows
  private final int[] m_subtreeEnds;   // inclusive last nonblank subtree indexes, -1 for blank rows

  /**************************************** constructor ******************************************/
  TaskHierarchy( List<Task> tasks )
  {
    this( tasks, null );
  }

  /**************************************** constructor ******************************************/
  TaskHierarchy( List<Task> tasks, int[] indents )
  {
    this( tasks, indents, -1, false );
  }

  /**************************************** constructor ******************************************/
  TaskHierarchy( List<Task> tasks, int[] indents, int titleRow, boolean titleBlank )
  {
    // supplied indentation allows a proposed outline to be checked without changing live task inputs
    // task 0 is the structural root even when its title is blank
    int size = tasks.size();
    if ( size == 0 || tasks.get( 0 ) == null )
      throw new IllegalStateException( "Task 0 is required as the project root" );
    if ( ( indents == null ? tasks.get( 0 ).getIndent() : indents[0] ) != 0 )
      throw new IllegalStateException( "Task 0 must have indentation zero" );

    m_parentIndices = new int[size];
    m_subtreeEnds = new int[size];
    Arrays.fill( m_parentIndices, -1 );
    Arrays.fill( m_subtreeEnds, -1 );

    // each nonblank task is pushed and popped once, keeping construction linear in row count
    int[] ancestors = new int[size];
    int depth = 0;
    int lastTask = 0;
    for ( int index = 1; index < size; index++ )
    {
      Task task = tasks.get( index );
      if ( task == null )
        throw new IllegalStateException( "Task " + index + " is missing; blank rows require a task object" );
      if ( index == titleRow ? titleBlank : task.isBlank() )
        continue;

      // the active chain must contain a parent for the requested level, including after blank rows
      int indent = indents == null ? task.getIndent() : indents[index];
      if ( indent < 0 || indent > depth )
        throw new IllegalStateException(
            "Task " + index + " has indentation " + indent + "; expected a level from 0 to " + depth );

      // close completed subtrees at the last nonblank task, excluding trailing blank rows
      while ( depth > indent )
        m_subtreeEnds[ancestors[--depth]] = lastTask;

      m_parentIndices[index] = indent == 0 ? 0 : ancestors[indent - 1];
      ancestors[depth++] = index;
      lastTask = index;
    }

    // close the final chain and root without retaining references to the mutable task list
    while ( depth > 0 )
      m_subtreeEnds[ancestors[--depth]] = lastTask;
    m_subtreeEnds[0] = lastTask;
  }

  /*************************************** getParentIndex ****************************************/
  public int getParentIndex( int index )
  {
    // indexes refer to the model order at construction, not the current visible row order
    return m_parentIndices[index];
  }

  /**************************************** hasChildren ******************************************/
  public boolean hasChildren( int index )
  {
    return m_subtreeEnds[index] > index;
  }

  /************************************* getSubtreeEndIndex **************************************/
  public int getSubtreeEndIndex( int index )
  {
    // leaves end at themselves; blank rows return -1; invalid indexes throw an index-bounds exception
    return m_subtreeEnds[index];
  }
}
