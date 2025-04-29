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

import rjc.jplanner.Main;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/*************************** Holds the complete data model for a plan ****************************/
/*************************************************************************************************/

public class Plan
{
  private String    m_title;          // plan title
  private DateTime  m_start;          // plan default start date-time for scheduling
  private Calendar  m_calendar;       // plan default calendar
  private String    m_datetimeFormat; // format to display date-times
  private String    m_dateFormat;     // format to display dates
  private String    m_filename;       // filename when saved or loaded
  private String    m_fileLocation;   // file location
  private String    m_savedBy;        // who saved last
  private DateTime  m_savedWhen;      // when was last saved
  private String    m_notes;          // plan notes

  private Tasks     tasks;            // list of plan tasks
  private Resources resources;        // list of plan resources
  private Calendars calendars;        // list of plan calendars
  private Days      daytypes;         // list of plan day types

  /**************************************** constructor ******************************************/
  public Plan()
  {
    // construct empty but usable Plan
    tasks = new Tasks();
    resources = new Resources();
    calendars = new Calendars();
    daytypes = new Days();

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

  /**************************************** initialise *******************************************/
  public void initialise()
  {
    // initialise plan with default settings and contents
    daytypes.initialise();
    calendars.initialise();
    resources.initialise();
    tasks.initialise();

    // default calendar to first calendar (should be standard week)
    m_calendar = getCalendar( 0 );
    m_start = DateTime.now();
  }

  /******************************************** task *********************************************/
  static public Task getTask( int index )
  {
    // return task corresponding to index
    return Main.getPlan().tasks.get( index );
  }

  static public int getIndex( Task task )
  {
    return Main.getPlan().tasks.indexOf( task );
  }

  public static Tasks getTasks()
  {
    return Main.getPlan().tasks;
  }

  /****************************************** resource *******************************************/
  static public Resource getResource( int index )
  {
    // return resource corresponding to index
    return Main.getPlan().resources.get( index );
  }

  static public int getIndex( Resource resource )
  {
    return Main.getPlan().resources.indexOf( resource );
  }

  public static Resources getResources()
  {
    return Main.getPlan().resources;
  }

  /****************************************** calendar *******************************************/
  static public Calendar getCalendar( int index )
  {
    // return calendar corresponding to index
    return Main.getPlan().calendars.get( index );
  }

  static public int getIndex( Calendar calendar )
  {
    return Main.getPlan().calendars.indexOf( calendar );
  }

  public static Calendars getCalendars()
  {
    return Main.getPlan().calendars;
  }

  /********************************************* day *********************************************/
  static public Day getDay( int index )
  {
    // return day-type corresponding to index
    return Main.getPlan().daytypes.get( index );
  }

  static public int getIndex( Day dayType )
  {
    return Main.getPlan().daytypes.indexOf( dayType );
  }

  public static Days getDays()
  {
    return Main.getPlan().daytypes;
  }

  /****************************************** getTitle *******************************************/
  public String getTitle()
  {
    return m_title;
  }

  /****************************************** getNotes *******************************************/
  public String getNotes()
  {
    return m_notes;
  }

  /*************************************** getDefaultStart ***************************************/
  public DateTime getDefaultStart()
  {
    return m_start;
  }

  /************************************** getDefaultCalendar *************************************/
  public Calendar getDefaultCalendar()
  {
    return m_calendar;
  }

  /************************************** getDateTimeFormat **************************************/
  public String getDateTimeFormat()
  {
    return m_datetimeFormat;
  }

  /**************************************** getDateFormat ****************************************/
  public String getDateFormat()
  {
    return m_dateFormat;
  }

  /***************************************** getFilename *****************************************/
  public String getFilename()
  {
    return m_filename;
  }

  /*************************************** getFileLocation ***************************************/
  public String getFileLocation()
  {
    return m_fileLocation;
  }

  /***************************************** getSavedBy ******************************************/
  public String getSavedBy()
  {
    return m_savedBy;
  }

  /**************************************** getSavedWhen *****************************************/
  public DateTime getSavedWhen()
  {
    return m_savedWhen;
  }

  /******************************************* setNotes ******************************************/
  public void setNotes( String notes )
  {
    m_notes = notes;
  }

  /******************************************* setTitle ******************************************/
  public void setTitle( String title )
  {
    m_title = title;
  }

  /*************************************** setDefaultStart ***************************************/
  public void setDefaultStart( DateTime start )
  {
    m_start = start;
  }

  /************************************** setDefaultCalendar *************************************/
  public void setDefaultCalendar( Calendar calendar )
  {
    m_calendar = calendar;
  }

  /*************************************** setDateTimeFormat *************************************/
  public void setDateTimeFormat( String DTformat )
  {
    m_datetimeFormat = DTformat;
  }

  /**************************************** setDateFormat ****************************************/
  public void setDateFormat( String Dformat )
  {
    m_dateFormat = Dformat;
  }

}
