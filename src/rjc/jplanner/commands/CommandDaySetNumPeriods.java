/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import rjc.jplanner.plan.Day;
import rjc.jplanner.plan.DayWorkPeriod;
import rjc.jplanner.plan.Plan;
import rjc.table.data.TableData;
import rjc.table.data.types.Time;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/****************** UndoCommand for updating day-types number of work periods ********************/
/*************************************************************************************************/

public class CommandDaySetNumPeriods implements IUndoCommand
{
  private TableData                m_data;       // table data
  private int                      m_dataRow;    // row in table data (Day)
  private ArrayList<DayWorkPeriod> m_newPeriods; // new list of work-periods after command
  private ArrayList<DayWorkPeriod> m_oldPeriods; // old list of work-periods before command
  private String                   m_text;       // text describing command

  /**************************************** constructor ******************************************/
  public CommandDaySetNumPeriods( TableData tableData, int dataRow, int newNum )
  {
    // initialise private variables
    Day day = Plan.getDay( dataRow ); // TODO IN FUTURE Day day = tableData.getDataSource() ....
    m_oldPeriods = day.getWorkPeriods();
    m_newPeriods = new ArrayList<DayWorkPeriod>( m_oldPeriods );
    int oldNum = m_oldPeriods.size();

    if ( newNum == oldNum )
      return;
    if ( newNum > oldNum )
    {
      // need to add new work-periods
      double remainingHours = 24.0;
      if ( !m_newPeriods.isEmpty() )
        remainingHours -= 24.0 * m_newPeriods.get( oldNum - 1 ).m_end.getDayMilliseconds() / Time.MILLISECONDS_IN_DAY;

      double increment = remainingHours / ( 1 + 2 * ( newNum - oldNum ) );
      if ( increment >= 8.0 )
        increment = 8.0;
      else if ( increment >= 4.0 )
        increment = 4.0;
      else if ( increment >= 2.0 )
        increment = 2.0;
      else if ( increment >= 1.0 )
        increment = 1.0;
      else if ( increment >= 0.5 )
        increment = 0.5;
      else if ( increment >= 10.0 / 60.0 )
        increment = 10.0 / 60.0;
      else if ( increment >= 5.0 / 60.0 )
        increment = 5.0 / 60.0;
      else
        increment = 1.0 / 60.0;

      double start = 24.0 - remainingHours + increment;

      for ( int count = oldNum; count < newNum; count++ )
      {
        m_newPeriods.add( new DayWorkPeriod( start, start + increment ) );
        start += 2 * increment;
      }
    }
    else
    {
      // need to reduce number of work-periods
      for ( int count = oldNum - 1; count >= newNum; count-- )
        m_newPeriods.remove( count );
    }

    // initialise private variables and action the command
    m_data = tableData;
    m_dataRow = dataRow;
    redo();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    m_data.setValue( Day.FIELD.Periods.ordinal(), m_dataRow, m_newPeriods );

  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    m_data.setValue( Day.FIELD.Periods.ordinal(), m_dataRow, m_oldPeriods );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // text description of command
    if ( m_text == null )
      m_text = "Day " + ( m_dataRow + 1 ) + " periods = " + m_newPeriods.size();

    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is only ready and valid when pointer to data is set
    return m_data != null;
  }

}
