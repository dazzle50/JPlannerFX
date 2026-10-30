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

import java.util.HashMap;
import java.util.Map;

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.gui.calendars.CalendarsData;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.calendars.Calendars;
import rjc.jplanner.plan.days.DayType;
import rjc.table.data.types.Date;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/*********************** Undo command for replacing a calendar's exceptions **********************/
/*************************************************************************************************/

public class CommandCalendarSetExceptions implements IUndoCommand
{
  private final CalendarsData      m_data;          // calendar table to update and notify
  private final Calendars          m_calendars;     // calendar collection used to resolve current column
  private final Calendar           m_calendar;      // same calendar even after column reordering
  private final Map<Date, DayType> m_oldExceptions; // independent snapshot before the edit
  private final Map<Date, DayType> m_newExceptions; // independent snapshot after the edit
  private boolean                  m_valid;         // initial action succeeded and changed contents

  /**************************************** constructor ******************************************/
  public CommandCalendarSetExceptions( CalendarsData data, int dataColumn, Map<Date, DayType> exceptions )
  {
    m_data = data;
    m_calendars = ( (PlanContext) data.getUserData() ).getPlan().getCalendars();
    m_calendar = m_calendars.get( dataColumn );
    m_oldExceptions = new HashMap<>( m_calendar.getExceptions() );
    m_newExceptions = new HashMap<>( exceptions );

    // compare contents, not the displayed count; the stack expects an already-executed command
    if ( !m_oldExceptions.equals( m_newExceptions ) )
    {
      apply( m_newExceptions );
      m_valid = true;
    }
  }

  /******************************************** apply ********************************************/
  private void apply( Map<Date, DayType> exceptions )
  {
    // locate by identity so reordering calendar columns cannot redirect undo to another calendar
    int dataColumn = m_calendars.indexOf( m_calendar );
    String error = m_data.setValue( dataColumn, Calendar.FIELD.Exceptions.ordinal(), exceptions );
    if ( error != null )
      throw new IllegalArgumentException( error );
  }

  /******************************************** redo *********************************************/
  @Override
  public void redo()
  {
    apply( m_newExceptions );
  }

  /******************************************** undo *********************************************/
  @Override
  public void undo()
  {
    // restore saved entries with the original day-type objects so plan references remain valid
    apply( m_oldExceptions );
  }

  /******************************************** text *********************************************/
  @Override
  public String text()
  {
    return "Calendar '" + m_calendar.getName() + "' exceptions";
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    return m_valid;
  }
}
