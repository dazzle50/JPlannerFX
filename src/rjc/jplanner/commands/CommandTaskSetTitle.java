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
import rjc.jplanner.plan.tasks.Task;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.jplanner.plan.tasks.Tasks.TitleState;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/*********************** Undo command for a task title and its outline level **********************/
/*************************************************************************************************/

public final class CommandTaskSetTitle implements IUndoCommand
{
  private final TasksData  m_data;     // task table to update and notify
  private final Tasks      m_tasks;    // task collection captured from the original plan context
  private final int        m_dataRow;  // original model position
  private final Task       m_task;     // same task object throughout history
  private final TitleState m_oldState; // exact title and level before editing
  private final TitleState m_newState; // validated title and contextual level after editing
  private boolean          m_valid;    // initial execution changed the task

  /**************************************** constructor ******************************************/
  public CommandTaskSetTitle( TasksData data, int dataRow, Object value )
  {
    // changes a task’s title and contextual indentation together, so undo restores both values
    // and can reverse task creation despite normal title-clearing restrictions
    if ( data == null )
      throw new IllegalArgumentException( "Task data must not be null" );

    m_data = data;
    m_dataRow = dataRow;
    m_tasks = ( (PlanContext) data.getUserData() ).getPlan().getTasks();
    m_newState = m_tasks.proposedTitle( dataRow, value );
    m_task = m_tasks.get( dataRow );
    m_oldState = new TitleState( (String) m_task.getValue( Task.FIELD.Title.ordinal() ), m_task.getIndent() );
    apply( m_newState );
    m_valid = !m_oldState.equals( m_newState );
  }

  /******************************************* apply *********************************************/
  private void apply( TitleState state )
  {
    // apply a title state to the task and notify the table of the change
    if ( m_oldState.equals( m_newState ) )
      return;
    if ( m_dataRow >= m_tasks.size() || m_tasks.get( m_dataRow ) != m_task )
      throw new IllegalStateException( "Task title history no longer matches the plan" );
    m_tasks.setTitle( m_dataRow, state );
    m_data.signalTableChanged();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    apply( m_newState );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // restoration does not reapply the initial user-clearing restriction or contextual defaults
    apply( m_oldState );
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
    return "Task " + m_dataRow + " title = " + m_newState.title();
  }
}
