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

package rjc.jplanner.plan;

import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/*************************** Holds the complete data model for a plan ****************************/
/*************************************************************************************************/

public class Plan
{
  private String    m_title;          // plan title as set in properties
  private DateTime  m_start;          // plan start date-time as set in properties
  private Calendar  m_calendar;       // plan's default calendar
  private String    m_datetimeFormat; // format to display date-times
  private String    m_dateFormat;     // format to display dates
  private String    m_filename;       // filename when saved or loaded
  private String    m_fileLocation;   // file location
  private String    m_savedBy;        // who saved last
  private DateTime  m_savedWhen;      // when was last saved
  private String    m_notes;          // plan notes as on plan tab

  private UndoStack m_undostack;      // undo stack for plan editing

  public Tasks      tasks;            // list of plan tasks
  public Resources  resources;        // list of plan resources
  public Calendars  calendars;        // list of plan calendars
  public Days       daytypes;         // list of plan day types

  /**************************************** constructor ******************************************/
  public Plan()
  {
    // construct empty but usable Plan
    tasks = new Tasks();
    resources = new Resources();
    calendars = new Calendars();
    daytypes = new Days();

    m_undostack = new UndoStack();

    m_title = "";
    m_datetimeFormat = "EEE dd/MM/yyyy HH:mm";
    m_dateFormat = "dd/MM/yyyy";
    m_filename = "";
    m_fileLocation = "";
    m_savedBy = "";
    m_notes = "";
  }

  /***************************************** toString ********************************************/
  @Override
  public String toString()
  {
    // return plan summary information
    return Utils.name( this ) + "[" + m_title + ", " + m_start + ", " + tasks.size() + " Tasks, " + resources.size()
        + " Resources, " + calendars.size() + " Calendars, " + daytypes.size() + " DayTypes]";
  }
}
