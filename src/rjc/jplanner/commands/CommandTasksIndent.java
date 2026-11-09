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

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.tasks.TasksData;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/******************** Undo command for changing selected task outline levels *********************/
/*************************************************************************************************/

public final class CommandTasksIndent implements IUndoCommand
{
  private final TasksData m_data;       // task table data to update and notify
  private final Tasks     m_tasks;      // task collection captured from the original plan context
  private final int[]     m_dataRows;   // changed model indexes, not visible row indexes
  private final int[]     m_oldIndents; // original levels matching the changed indexes
  private final int       m_direction;  // one level inward (+1) or outward (-1)
  private boolean         m_valid;      // initial execution succeeded and changed levels

  /**************************************** constructor ******************************************/
  public CommandTasksIndent( TasksData data, int[] dataRows, int direction )
  {
    // changes selected tasks’ and descendants’ indentation together, saving exact affected rows
    // and levels because applying the opposite action could include newly acquired children
    if ( data == null )
      throw new IllegalArgumentException( "Task data must not be null" );
    m_data = data;
    m_tasks = ( (PlanContext) data.getUserData() ).getPlan().getTasks();
    m_direction = direction;

    // validation errors propagate before mutation so the caller can report the conflict
    int[] proposal = m_tasks.proposedIndents( dataRows, direction );
    int count = 0;
    for ( int row = 0; row < proposal.length; row++ )
      if ( proposal[row] != m_tasks.get( row ).getIndent() )
        count++;

    // retain only changed indexes and original levels; full proposals remain temporary
    m_dataRows = new int[count];
    m_oldIndents = new int[count];
    int index = 0;
    for ( int row = 0; row < proposal.length; row++ )
      if ( proposal[row] != m_tasks.get( row ).getIndent() )
      {
        m_dataRows[index] = row;
        m_oldIndents[index++] = m_tasks.get( row ).getIndent();
      }

    apply( true );
    m_valid = count > 0;
  }

  /******************************************* apply *********************************************/
  private void apply( boolean forward )
  {
    // apply the indent change to the selected tasks and their descendants, either forward or backward
    if ( m_dataRows.length == 0 )
      return;

    // linear history restores model positions before replay; untracked reordering is unsupported
    for ( int row : m_dataRows )
      if ( row <= 0 || row >= m_tasks.size() )
        throw new IllegalStateException( "Task history row no longer exists: " + row );

    int[] proposal = new int[m_tasks.size()];
    for ( int row = 0; row < proposal.length; row++ )
      proposal[row] = m_tasks.get( row ).getIndent();

    // restore exact saved targets, not an inverse action that could include newly acquired children
    for ( int index = 0; index < m_dataRows.length; index++ )
      proposal[m_dataRows[index]] = m_oldIndents[index] + ( forward ? m_direction : 0 );

    m_tasks.setIndents( proposal );
    m_data.signalTableChanged();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    apply( false );
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    apply( true );
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
    return m_direction == 1 ? "Indent tasks" : "Outdent tasks";
  }
}
