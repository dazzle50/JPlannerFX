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

import rjc.jplanner.plan.Calendar;
import rjc.jplanner.plan.Day;
import rjc.jplanner.plan.Plan;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/************************ UndoCommand for updating calendar cycle length *************************/
/*************************************************************************************************/

public class CommandCalendarSetCycleLength implements IUndoCommand
{
  private TableData      m_data;       // table data
  private int            m_dataColumn; // column in table data (Calendar)
  private ArrayList<Day> m_newNormals; // new list of normal-cycle-days after command
  private ArrayList<Day> m_oldNormals; // old list of normal-cycle-days before command
  private String         m_text;       // text describing command

  /**************************************** constructor ******************************************/
  public CommandCalendarSetCycleLength( TableData tableData, int dataColumn, int newLength )
  {
    // initialise private variables
    Calendar calendar = Plan.getCalendar( dataColumn ); // TODO IN FUTURE Day day = tableData.getDataSource() ....
    m_oldNormals = calendar.getNormals();
    m_newNormals = new ArrayList<Day>( m_oldNormals );
    int oldLength = m_oldNormals.size();

    if ( newLength == oldLength )
      return;
    if ( newLength > oldLength )
    {
      // need to add new normal-cycle-days
      Day day = m_oldNormals.getLast();
      for ( int count = oldLength; count < newLength; count++ )
        m_newNormals.add( day );
    }
    else
    {
      // need to reduce number of normal-cycle-days
      for ( int count = oldLength - 1; count >= newLength; count-- )
        m_newNormals.remove( count );
    }

    // initialise private variables and action the command
    m_data = tableData;
    m_dataColumn = dataColumn;
    redo();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    m_data.setValue( m_dataColumn, Calendar.FIELD.Cycle.ordinal(), m_newNormals );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    // m_calendar.setNormals( m_oldNormals );
    m_data.setValue( m_dataColumn, Calendar.FIELD.Cycle.ordinal(), m_oldNormals );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // text description of command
    if ( m_text == null )
      m_text = "Calendar " + ( m_dataColumn + 1 ) + " Cycle = " + m_newNormals.size();

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
