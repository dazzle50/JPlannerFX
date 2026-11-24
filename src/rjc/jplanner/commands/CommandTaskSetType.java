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
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.jplanner.plan.tasks.TaskTiming;
import rjc.jplanner.plan.tasks.TaskType;
import rjc.jplanner.plan.tasks.Tasks;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/********************** Undo command for a task type and its timing details **********************/
/*************************************************************************************************/

public final class CommandTaskSetType implements IUndoCommand
{
  private final TasksData  m_data;      // task table to update and notify
  private final Tasks      m_tasks;     // task collection captured from the original plan context
  private final int        m_dataRow;   // original model position
  private final Task       m_task;      // same task object throughout history
  private final TaskType   m_oldType;   // task type before editing
  private final TaskType   m_newType;   // task type after editing
  private final TaskTiming m_oldTiming; // exact timing before editing
  private final TaskTiming m_newTiming; // exact timing after editing
  private final boolean    m_valid;     // initial execution changed the task type

  /**************************************** constructor ******************************************/
  public CommandTaskSetType( TasksData data, int dataRow, TaskType type )
  {
    if ( data == null )
      throw new IllegalArgumentException( "Task data must not be null" );

    // retain the table's validation, including its protection of summary tasks
    String problem = data.testValue( FIELD.Type.ordinal(), dataRow, type );
    if ( problem != null )
      throw new IllegalArgumentException( problem );

    m_data = data;
    m_dataRow = dataRow;
    m_tasks = ( (PlanContext) data.getUserData() ).getPlan().getTasks();
    m_task = m_tasks.get( dataRow );
    m_oldType = m_task.getTaskType();
    m_newType = type;
    m_oldTiming = m_task.getTiming();
    m_valid = m_oldType != m_newType;

    // apply normal status-clearing rules once; unchanged choices must not touch data or history
    if ( m_valid )
    {
      problem = m_data.setValue( FIELD.Type.ordinal(), m_dataRow, m_newType );
      if ( problem != null )
        throw new IllegalArgumentException( problem );
    }
    m_newTiming = m_task.getTiming();
  }

  /******************************************* apply *********************************************/
  private void apply( TaskType type, TaskTiming timing )
  {
    if ( !m_valid )
      return;

    // linear history restores model positions before replay; never update another row's task
    if ( m_dataRow >= m_tasks.size() || m_tasks.get( m_dataRow ) != m_task )
      throw new IllegalStateException( "Task type history no longer matches the plan" );

    // restore both saved values before notifying views so they cannot observe a partial restoration
    m_task.restoreTypeAndTiming( type, timing );
    m_data.signalRowChanged( m_dataRow );
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    apply( m_newType, m_newTiming );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    apply( m_oldType, m_oldTiming );
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
    return "Task " + m_dataRow + " type = " + m_newType;
  }
}
