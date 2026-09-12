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

package rjc.jplanner.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rjc.jplanner.gui.days.DaysData;
import rjc.table.HashSetInt;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************* UndoCommand for inserting plan day-types ******************************/
/*************************************************************************************************/

public final class CommandInsertDayTypes implements IUndoCommand
{
  // contiguous insertion at an original data index
  private record Run( int index, List<Object> rows )
  {
  }

  private final DaysData  m_data;                     // table data
  private final TableAxis m_axis;                     // table rows axis
  private final List<Run> m_runs = new ArrayList<>(); // insertion runs in highest-index-first order
  private final int       m_count;                    // total number of day-types inserted

  /**************************************** constructor ******************************************/
  public CommandInsertDayTypes( TableView view, HashSetInt viewIndexes )
  {
    // initialise private variables
    m_data = (DaysData) view.getData();
    m_axis = view.getRowsAxis();
    int[] indexes = viewIndexes.toArray();

    // convert view indexes to data indexes before insertion, preserving the append position
    for ( int i = 0; i < indexes.length; i++ )
    {
      java.util.Objects.checkIndex( indexes[i], m_axis.getCount() + 1 );
      if ( indexes[i] != m_axis.getCount() )
        indexes[i] = m_axis.getDataIndex( indexes[i] );
    }
    Arrays.sort( indexes );
    m_count = indexes.length;

    // reserve unique names across all insertion runs
    var rows = m_data.prepareRows( Collections.nCopies( m_count, null ) );

    // group adjacent data indexes for table size and mapping updates
    for ( int first = 0; first < m_count; )
    {
      int end = first + 1;
      while ( end < m_count && indexes[end] == indexes[end - 1] + 1 )
        end++;
      m_runs.add( new Run( indexes[first], List.copyOf( rows.subList( first, end ) ) ) );
      first = end;
    }

    // action command from highest to lowest index to avoid shifting remaining insertion positions
    Collections.reverse( m_runs );
    if ( isValid() )
      redo();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    for ( Run run : m_runs )
    {
      // restore the same day objects so calendar references remain valid
      if ( !m_data.insertRows( run.index(), run.rows() ) )
        throw new IllegalStateException( "Could not restore inserted day types" );

      // update row sizes and mapping explicitly after the row count changes
      m_axis.insertSizes( run.index(), run.rows().size() );
      m_axis.insertMapping( run.index(), run.rows().size() );
    }

    m_data.signalTableChanged();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command in reverse insertion order to restore each run's original index
    for ( int i = m_runs.size() - 1; i >= 0; i-- )
    {
      Run run = m_runs.get( i );
      m_axis.deleteMapping( run.index(), run.rows().size() );
      m_axis.deleteSizes( run.index(), run.rows().size() );
      List<Object> removed = m_data.deleteRows( run.index(), run.rows().size() );

      // check removed rows are the original day objects
      if ( !removed.equals( run.rows() ) )
        throw new IllegalStateException( "Inserted day identities changed before undo" );
    }

    m_data.signalTableChanged();
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is only valid when at least one day-type is inserted
    return m_count > 0;
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // text description of command
    return "Insert " + m_count + ( m_count == 1 ? " day type" : " day types" );
  }

}
