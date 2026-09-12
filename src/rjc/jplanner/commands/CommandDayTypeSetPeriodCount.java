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

import java.util.List;

import rjc.jplanner.gui.days.DaysData;
import rjc.jplanner.plan.days.WorkPeriod;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/****************** UndoCommand for updating day-types number of work periods ********************/
/*************************************************************************************************/

public final class CommandDayTypeSetPeriodCount implements IUndoCommand
{
  private final DaysData         m_data;       // table data
  private final int              m_dataRow;    // row in table data (DayType)
  private final List<WorkPeriod> m_oldPeriods; // immutable work-periods before command
  private final List<WorkPeriod> m_newPeriods; // immutable work-periods after command
  private boolean                m_valid;      // whether command was successfully applied

  /**************************************** constructor ******************************************/
  public CommandDayTypeSetPeriodCount( DaysData data, int row, int count )
  {
    // initialise private variables
    m_data = data;
    m_dataRow = row;

    // snapshot existing work-periods without generating new boundaries
    m_oldPeriods = m_data.proposedPeriodCount( m_dataRow,
        (int) m_data.getValue( DaysData.FIELD.Periods.ordinal(), m_dataRow ) );
    List<WorkPeriod> proposal;
    try
    {
      proposal = m_data.proposedPeriodCount( m_dataRow, count );
    }
    catch ( IllegalArgumentException | java.time.DateTimeException exception )
    {
      // reject invalid proposal without changing the day or creating an undo entry
      m_newPeriods = m_oldPeriods;
      return;
    }
    m_newPeriods = proposal;

    // action command only if work-periods have changed
    if ( !m_oldPeriods.equals( m_newPeriods ) )
    {
      String problem = m_data.setValue( DaysData.FIELD.Periods.ordinal(), m_dataRow, m_newPeriods );

      // mark command valid for the undo-stack only after successful application
      if ( problem == null )
      {
        m_valid = true;
        m_data.signalRowChanged( m_dataRow );
      }
    }
  }

  /******************************************* apply *********************************************/
  private void apply( List<WorkPeriod> periods )
  {
    // restore validated work-periods, reporting any inconsistent undo history or state
    String problem = m_data.setValue( DaysData.FIELD.Periods.ordinal(), m_dataRow, periods );
    if ( problem != null )
      throw new IllegalStateException( problem );

    m_data.signalRowChanged( m_dataRow );
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    apply( m_newPeriods );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    apply( m_oldPeriods );
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is only ready and valid after successful application
    return m_valid;
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // text description of command
    return "Day " + ( m_dataRow + 1 ) + " periods = " + m_newPeriods.size();
  }

}
