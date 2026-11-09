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

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksData;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.jplanner.plan.tasks.Tasks.DeletionState;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************** Undo command for task groups, links, and row geometry **********************/
/*************************************************************************************************/

public final class CommandTasksDelete implements IUndoCommand
{
  private final TasksData     m_data;    // table data whose complete row count is updated
  private final Tasks         m_tasks;   // task collection captured from the original plan context
  private final TableAxis     m_axis;    // row sizes and mappings to preserve
  private final DeletionState m_state;   // removed task objects and changed dependency values
  private final int[][]       m_runs;    // ascending original model ranges, start and count
  private final short[][]     m_sizes;   // captured nominal sizes, including hidden markers
  private final int[][][]     m_mapping; // captured mapping entries for each deleted range
  private final int           m_count;   // number of deleted rows, including interior blanks
  private boolean             m_valid;   // initial execution removed rows

  /**************************************** constructor ******************************************/
  public CommandTasksDelete( TasksData data, TableAxis axis, int[] dataRows )
  {
    // deletes complete task groups and removes dependencies on them, preserving task objects, links,
    // and row geometry so undo restores the original state
    if ( data == null || axis == null )
      throw new IllegalArgumentException( "Task data and row axis are required" );

    m_data = data;
    m_tasks = ( (PlanContext) data.getUserData() ).getPlan().getTasks();
    m_axis = axis;
    m_state = m_tasks.prepareDeletion( dataRows );
    int[] rows = m_state.getDataRows();
    m_count = rows.length;

    var runs = new ArrayList<int[]>();
    for ( int index = 0; index < rows.length; index++ )
      if ( index == 0 || rows[index] != rows[index - 1] + 1 )
        runs.add( new int[] { rows[index], 1 } );
      else
        runs.getLast()[1]++;

    m_runs = runs.toArray( int[][]::new );
    m_sizes = new short[m_runs.length][];
    m_mapping = new int[m_runs.length][][];
    redo();
    m_valid = m_count > 0;
  }

  /****************************************** validate *******************************************/
  private void validate( boolean restore )
  {
    // validates that the current state of the table matches the expected state for undo or redo
    if ( m_data.getRowCount() != m_tasks.size() || m_axis.getCount() != m_data.getRowCount() )
      throw new IllegalStateException( "Task row count and geometry are inconsistent" );

    // all predictable rejection occurs before modifying model, sizes, or mapping
    m_tasks.validateDeletion( m_state, restore );
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // removes the task groups and links, and updates the row geometry
    if ( m_count == 0 )
      return;
    validate( false );
    for ( int index = m_runs.length - 1; index >= 0; index-- )
    {
      m_mapping[index] = m_axis.deleteMapping( m_runs[index][0], m_runs[index][1] );
      m_sizes[index] = m_axis.deleteSizes( m_runs[index][0], m_runs[index][1] );
    }
    m_tasks.applyDeletion( m_state, false );

    // tasks and links are complete before count observers run; sizes were shifted before truncation
    m_data.setRowCount( m_tasks.size() );
    m_data.signalTableChanged();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // restores the task groups and links, and restores the row geometry
    if ( m_count == 0 )
      return;
    validate( true );
    m_tasks.applyDeletion( m_state, true );
    m_data.setRowCount( m_tasks.size() );

    // restore low-to-high after the data count increases, following the table axis contract
    for ( int index = 0; index < m_runs.length; index++ )
    {
      m_axis.insertSizes( m_runs[index][0], m_sizes[index] );
      m_axis.insertMapping( m_runs[index][0], m_runs[index][1], m_mapping[index] );
    }
    m_data.signalTableChanged();
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    return m_valid;
  }

  /******************************************** text *********************************************/
  @Override
  public String text()
  {
    return "Delete " + m_count + " task rows";
  }
}
