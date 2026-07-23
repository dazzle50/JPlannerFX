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

package rjc.jplanner.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.stream.XMLStreamReader;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.calenders.Calendar;
import rjc.jplanner.plan.days.Day;
import rjc.jplanner.plan.days.DayWorkPeriod;
import rjc.jplanner.plan.resources.Resource;
import rjc.jplanner.plan.tasks.Task;
import rjc.table.Utils;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************************* Reading plan data from XML files ********************************/
/*************************************************************************************************/

public final class XmlPlanReader
{
  private Plan            m_plan;         // plan currently being populated
  private XMLStreamReader m_xml;          // underlying StAX cursor for this read
  private String          m_calendarName; // name of default calendar, resolved once loading finishes

  /******************************************** read *********************************************/
  public Plan read( XMLStreamReader xml ) throws Exception
  {
    // reader must be positioned on the plan-data start element
    if ( !xml.isStartElement() || !xml.getLocalName().equals( XmlLabels.XML_PLAN_DATA ) )
      throw new IOException( "XML reader is not positioned on '" + XmlLabels.XML_PLAN_DATA + "'" );

    m_plan = new Plan();
    m_xml = xml;
    m_calendarName = null;

    // replace existing plan contents while retaining the Plan owned by PlanContext
    m_plan.getDays().clear();
    m_plan.getCalendars().clear();
    m_plan.getResources().clear();
    m_plan.getTasks().clear();

    // plan-level attributes (title, default start, format strings etc.) sit on this start element
    processPlanAttributes();

    // process known sections, skip unknown sections, and continue after section failures
    while ( m_xml.hasNext() )
    {
      m_xml.next();

      // reached the closing tag for plan-data - loading is complete
      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_PLAN_DATA ) )
      {
        finishLoad();
        return m_plan;
      }

      // ignore text, comments, whitespace etc. between sections
      if ( !m_xml.isStartElement() )
        continue;

      String section = m_xml.getLocalName();

      try
      {
        // dispatch to the handler for this section, based on its element name
        switch ( section )
        {
          case XmlLabels.XML_DAY_DATA -> processDays();
          case XmlLabels.XML_CAL_DATA -> processCalendars();
          case XmlLabels.XML_RES_DATA -> processResources();
          case XmlLabels.XML_TASK_DATA -> processTasks();

          default -> {
            // unrecognised section name - trace and skip its whole subtree
            Utils.trace( "Unhandled element '" + section + "'" );
            skipElement( section );
          }
        }
      }
      catch ( Exception exception )
      {
        // stop the failed section, trace the problem, and continue with later sections
        Utils.trace( "Error processing element '" + section + "': " + exception.getMessage() );
        skipElement( section );
      }
    }

    // stream ended without ever finding the closing plan-data tag - file is truncated/corrupt
    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_PLAN_DATA + "'" );
  }

  /*********************************** processPlanAttributes *************************************/
  private void processPlanAttributes()
  {
    // process plan attributes updating the plan, except calendar which is handled later
    try
    {
      for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
      {
        String attrib = m_xml.getAttributeLocalName( i );
        String value = m_xml.getAttributeValue( i );

        switch ( attrib )
        {
          case XmlLabels.XML_TITLE -> m_plan.setTitle( value );
          case XmlLabels.XML_START -> m_plan.setDefaultStart( DateTime.parse( value ) );
          // calendar isn't loaded yet at this point, so just remember its name for later lookup
          case XmlLabels.XML_CALENDAR -> m_calendarName = value;
          case XmlLabels.XML_DT_FORMAT -> m_plan.setDateTimeFormat( value );
          case XmlLabels.XML_D_FORMAT -> m_plan.setDateFormat( value );
          case XmlLabels.XML_NOTES -> m_plan.setNotes( value );

          default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
        }
      }
    }
    catch ( Exception exception )
    {
      // one bad attribute must not prevent the rest of the plan from loading
      Utils.trace( "Error processing plan-data attributes: " + exception.getMessage() );
    }
  }

  /***************************************** processDays *****************************************/
  private void processDays() throws Exception
  {
    // process day definitions until the end of the day-data section
    while ( m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_DAY_DATA ) )
        return;

      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      if ( element.equals( XmlLabels.XML_DAY ) )
        processDay();
      else
      {
        // only day elements are expected directly inside day-data
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_DAY_DATA + "'" );
        skipElement( element );
      }
    }

    // ran out of stream before finding the closing tag - malformed xml
    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_DAY_DATA + "'" );
  }

  /****************************************** processDay *****************************************/
  private void processDay() throws Exception
  {
    // collect attributes for the current day definition
    String name = null;
    double work = Double.NaN;

    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );

      switch ( attrib )
      {
        // id is positional (list index) not stored on Day itself - only its format is checked here
        case XmlLabels.XML_ID -> Integer.parseInt( value );
        case XmlLabels.XML_NAME -> name = value;
        case XmlLabels.XML_WORK -> work = Double.parseDouble( value );
        default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
      }
    }

    // collect nested work periods contained by the current day definition
    var periods = new ArrayList<DayWorkPeriod>();

    while ( m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_DAY ) )
        break;

      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      if ( element.equals( XmlLabels.XML_PERIOD ) )
        periods.add( processPeriod() );
      else
      {
        // only period elements are expected directly inside a day
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_DAY + "'" );
        skipElement( element );
      }
    }

    // use Day validation when applying the collected values
    var day = new Day();
    String problem = day.setValue( Day.FIELD.Name.ordinal(), name, true );
    if ( problem == null )
      problem = day.setValue( Day.FIELD.Work.ordinal(), work, true );
    if ( problem == null )
      problem = day.setValue( Day.FIELD.Periods.ordinal(), periods, true );
    if ( problem != null )
      // any field rejected by validation aborts this day entirely, rather than adding a partial one
      throw new IOException( problem );

    m_plan.getDays().add( day );
  }

  /**************************************** processPeriod ****************************************/
  private DayWorkPeriod processPeriod() throws Exception
  {
    // read the current work-period attributes
    Time start = null;
    Time end = null;

    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );

      switch ( attrib )
      {
        // id is positional only - parsed solely to confirm it is a well-formed integer
        case XmlLabels.XML_ID -> Integer.parseInt( value );
        case XmlLabels.XML_START -> start = Time.parse( value );
        case XmlLabels.XML_END -> end = Time.parse( value );
        default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
      }
    }

    // ignore any unexpected child content before returning the period
    skipElement( XmlLabels.XML_PERIOD );
    return new DayWorkPeriod( start, end );
  }

  /************************************** processCalendars ***************************************/
  private void processCalendars() throws Exception
  {
    // process calendar definitions until the end of the calendar-data section
    int expectedId = 0;
    while ( m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_CAL_DATA ) )
        return;

      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      if ( !element.equals( XmlLabels.XML_CALENDAR ) )
      {
        // only calendar elements are expected directly inside calendar-data
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_CAL_DATA + "'" );
        skipElement( element );
        continue;
      }

      try
      {
        processCalendar( expectedId++ );
      }
      catch ( Exception exception )
      {
        // one bad calendar shouldn't stop the rest of the file loading
        Utils.trace( "Error processing calendar " + ( expectedId - 1 ) + ": " + exception.getMessage() );
        skipCurrentElement();
      }
    }

    // ran out of stream before finding the closing tag - malformed xml
    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_CAL_DATA + "'" );
  }

  /**************************************** processCalendar **************************************/
  private void processCalendar( int expectedId ) throws Exception
  {
    // collect attributes for the current calendar definition
    Integer id = null;
    String name = null;
    Date anchor = null;
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );

      switch ( attrib )
      {
        case XmlLabels.XML_ID -> id = Integer.parseInt( value );
        case XmlLabels.XML_NAME -> name = value;
        case XmlLabels.XML_ANCHOR -> anchor = Date.parse( value );
        default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
      }
    }

    // calendar ids must appear in file order - catches reordered or duplicated calendar blocks
    if ( id == null || id != expectedId )
      throw new IOException( "Calendar id must be " + expectedId + " but was " + id );

    var normals = new ArrayList<Day>();
    // last exception wins on a duplicate date - no explicit duplicate detection here
    var exceptions = new HashMap<Date, Day>();

    while ( m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_CALENDAR ) )
        break;

      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      try
      {
        if ( element.equals( XmlLabels.XML_NORMAL ) )
          // resolves a day-id reference into the actual Day instance already loaded earlier
          normals.add( m_plan.getDay( readDayReference() ) );
        else if ( element.equals( XmlLabels.XML_EXCEPTION ) )
        {
          Date date = null;
          Integer day = null;
          for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
          {
            String attrib = m_xml.getAttributeLocalName( i );
            String value = m_xml.getAttributeValue( i );
            if ( attrib.equals( XmlLabels.XML_DATE ) )
              date = Date.parse( value );
            else if ( attrib.equals( XmlLabels.XML_DAY ) )
              day = Integer.parseInt( value );
            else
              Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
          }
          if ( date == null || day == null )
            throw new IOException( "Calendar exception requires date and day" );
          exceptions.put( date, m_plan.getDay( day ) );
          skipElement( element );
        }
        else
        {
          // only normal and exception elements are expected directly inside a calendar
          Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_CALENDAR + "'" );
          skipElement( element );
        }
      }
      catch ( Exception exception )
      {
        // one bad normal-day or exception entry shouldn't discard the whole calendar
        Utils.trace( "Error processing calendar element '" + element + "': " + exception.getMessage() );
        skipCurrentElement();
      }
    }

    // a calendar with no working days at all is not usable for scheduling
    if ( normals.isEmpty() )
      throw new IOException( "Calendar requires at least one normal day" );

    var calendar = new Calendar( name, anchor, normals.toArray( new Day[0] ) );
    calendar.getExceptions().putAll( exceptions );
    m_plan.getCalendars().add( calendar );
  }

  /************************************** readDayReference ***************************************/
  private int readDayReference() throws Exception
  {
    // collect the day attribute from the current normal-day element
    Integer day = null;
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      if ( attrib.equals( XmlLabels.XML_DAY ) )
        day = Integer.parseInt( m_xml.getAttributeValue( i ) );
      else if ( !attrib.equals( XmlLabels.XML_ID ) )
        // id itself is expected but carries no useful value here, so it's silently ignored
        Utils.trace( "Unhandled attribute '" + attrib + "' = '" + m_xml.getAttributeValue( i ) + "'" );
    }
    // normal elements have no children, so this only consumes the closing tag
    skipElement( XmlLabels.XML_NORMAL );
    if ( day == null )
      throw new IOException( "Normal calendar day reference is missing" );
    return day;
  }

  /************************************** processResources ***************************************/
  private void processResources() throws Exception
  {
    // pre-size with blank resources so processResource can populate by id in any order
    int count = readCount();
    var resources = m_plan.getResources();
    resources.clear();
    for ( int i = 0; i < count; i++ )
      resources.add( new Resource() );

    // process individual resource elements until the end of the resource-data section
    while ( m_xml.hasNext() )
    {
      m_xml.next();
      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_RES_DATA ) )
      {
        // ids may have been applied out of order, so rebuild any cached id/name lookups now
        resources.invalidateLookup();
        return;
      }
      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      if ( !element.equals( XmlLabels.XML_RESOURCE ) )
      {
        // only resource elements are expected directly inside resource-data
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_RES_DATA + "'" );
        skipElement( element );
        continue;
      }

      try
      {
        processResource( count );
      }
      catch ( Exception exception )
      {
        // one bad resource leaves its slot blank rather than aborting the whole section
        Utils.trace( "Error processing resource: " + exception.getMessage() );
        skipCurrentElement();
      }
    }

    // ran out of stream before finding the closing tag - malformed xml
    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_RES_DATA + "'" );
  }

  /*************************************** processResource ***************************************/
  private void processResource( int count ) throws Exception
  {
    // reads a single resource element's attributes into the resource already pre-allocated at its id
    Integer id = null;
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
      if ( m_xml.getAttributeLocalName( i ).equals( XmlLabels.XML_ID ) )
        id = Integer.parseInt( m_xml.getAttributeValue( i ) );

    // id must resolve to one of the slots pre-allocated by processResources
    if ( id == null || id < 0 || id >= count )
      throw new IOException( "Invalid resource id: " + id );

    Resource resource = m_plan.getResource( id );
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );
      try
      {
        // each branch returns null on success, or a description of why the value was rejected
        String problem = switch ( attrib )
        {
          case XmlLabels.XML_ID -> null;
          case XmlLabels.XML_INITIALS -> resource.setValue( Resource.FIELD.Initials.ordinal(), value, true );
          case XmlLabels.XML_NAME -> resource.setValue( Resource.FIELD.Name.ordinal(), value, true );
          case XmlLabels.XML_ORG -> resource.setValue( Resource.FIELD.Organisation.ordinal(), value, true );
          case XmlLabels.XML_GROUP -> resource.setValue( Resource.FIELD.Group.ordinal(), value, true );
          case XmlLabels.XML_ROLE -> resource.setValue( Resource.FIELD.Role.ordinal(), value, true );
          case XmlLabels.XML_ALIAS -> resource.setValue( Resource.FIELD.Alias.ordinal(), value, true );
          case XmlLabels.XML_START -> resource.setValue( Resource.FIELD.Start.ordinal(), Date.parse( value ), true );
          case XmlLabels.XML_END -> resource.setValue( Resource.FIELD.End.ordinal(), Date.parse( value ), true );
          // calendar is referenced by id, resolved against calendars already loaded earlier
          case XmlLabels.XML_CALENDAR -> resource.setValue( Resource.FIELD.Calendar.ordinal(),
              m_plan.getCalendar( Integer.parseInt( value ) ), true );
          case XmlLabels.XML_COMMENT -> resource.setValue( Resource.FIELD.Comment.ordinal(), value, true );
          // not yet supported by the data model - trace so gaps in loaded data are visible
          case XmlLabels.XML_AVAIL, XmlLabels.XML_COST -> "TODO loading resource attribute '" + attrib + "'";
          default -> "Unhandled attribute '" + attrib + "' = '" + value + "'";
        };
        if ( problem != null )
          Utils.trace( "Resource " + id + ": " + problem );
      }
      catch ( Exception exception )
      {
        // a single bad attribute is dropped rather than losing the rest of the resource
        Utils.trace( "Resource " + id + " attribute '" + attrib + "': " + exception.getMessage() );
      }
    }
    // resource elements have no children, so this only consumes the closing tag
    skipElement( XmlLabels.XML_RESOURCE );
  }

  /**************************************** processTasks *****************************************/
  private void processTasks() throws Exception
  {
    // pre-size with blank tasks so processTask can populate by id in any order
    int count = readCount();
    var tasks = m_plan.getTasks();
    tasks.clear();
    for ( int i = 0; i < count; i++ )
      tasks.add( new Task() );

    // raw text collected per task id, applied only once every task exists (see below)
    var predecessors = new String[count];
    var resources = new String[count];

    while ( m_xml.hasNext() )
    {
      m_xml.next();
      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_TASK_DATA ) )
      {
        // now that every task slot is populated, cross-references can finally be resolved
        for ( int id = 0; id < count; id++ )
        {
          applyTaskReference( id, Task.FIELD.Predecessors, predecessors[id] );
          applyTaskReference( id, Task.FIELD.Resources, resources[id] );
        }
        return;
      }
      if ( !m_xml.isStartElement() )
        continue;

      String element = m_xml.getLocalName();
      if ( !element.equals( XmlLabels.XML_TASK ) )
      {
        // only task elements are expected directly inside task-data
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_TASK_DATA + "'" );
        skipElement( element );
        continue;
      }

      try
      {
        processTask( count, predecessors, resources );
      }
      catch ( Exception exception )
      {
        // one bad task leaves its slot blank rather than aborting the whole section
        Utils.trace( "Error processing task: " + exception.getMessage() );
        skipCurrentElement();
      }
    }

    // ran out of stream before finding the closing tag - malformed xml
    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_TASK_DATA + "'" );
  }

  /***************************************** processTask *****************************************/
  private void processTask( int count, String[] predecessors, String[] resources ) throws Exception
  {
    // read the current task's id attribute, which must resolve to one of the pre-allocated slots
    Integer id = null;
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
      if ( m_xml.getAttributeLocalName( i ).equals( XmlLabels.XML_ID ) )
        id = Integer.parseInt( m_xml.getAttributeValue( i ) );

    // id must resolve to one of the slots pre-allocated by processTasks
    if ( id == null || id < 0 || id >= count )
      throw new IOException( "Invalid task id: " + id );

    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );
      try
      {
        // each branch returns null on success, or a description of why the value was rejected
        String problem = switch ( attrib )
        {
          case XmlLabels.XML_ID -> null;
          case XmlLabels.XML_TITLE -> m_plan.getTasks().setValue( id, Task.FIELD.Title.ordinal(), value, true );
          case XmlLabels.XML_DURATION -> m_plan.getTasks().setValue( id, Task.FIELD.Duration.ordinal(),
              new TimeSpan( value ), true );
          case XmlLabels.XML_PRIORITY -> m_plan.getTasks().setValue( id, Task.FIELD.Priority.ordinal(),
              Integer.parseInt( value ), true );
          case XmlLabels.XML_START -> m_plan.getTasks().setValue( id, Task.FIELD.Start.ordinal(),
              DateTime.parse( value ), true );
          case XmlLabels.XML_END -> m_plan.getTasks().setValue( id, Task.FIELD.End.ordinal(), DateTime.parse( value ),
              true );
          case XmlLabels.XML_DEADLINE -> m_plan.getTasks().setValue( id, Task.FIELD.Deadline.ordinal(),
              DateTime.parse( value ), true );
          case XmlLabels.XML_COMMENT -> m_plan.getTasks().setValue( id, Task.FIELD.Comment.ordinal(), value, true );
          // deferred until all tasks/resources exist - see processTasks and applyTaskReference
          case XmlLabels.XML_PREDS -> {
            predecessors[id] = value;
            yield null;
          }
          case XmlLabels.XML_RESOURCES -> {
            resources[id] = value;
            yield null;
          }
          // not yet supported by the data model - trace so gaps in loaded data are visible
          case XmlLabels.XML_TYPE -> "TODO loading task type '" + value + "'";
          case XmlLabels.XML_WORK, XmlLabels.XML_COST, XmlLabels.XML_INDENT -> "TODO loading task attribute '" + attrib
              + "'";
          default -> "Unhandled attribute '" + attrib + "' = '" + value + "'";
        };
        if ( problem != null )
          Utils.trace( "Task " + id + ": " + problem );
      }
      catch ( Exception exception )
      {
        // a single bad attribute is dropped rather than losing the rest of the task
        Utils.trace( "Task " + id + " attribute '" + attrib + "': " + exception.getMessage() );
      }
    }
    // task elements have no children, so this only consumes the closing tag
    skipElement( XmlLabels.XML_TASK );
  }

  /************************************* applyTaskReference **************************************/
  private void applyTaskReference( int id, Task.FIELD field, String value )
  {
    // nothing was stashed for this task, so there's nothing to resolve
    if ( value == null )
      return;

    String problem = m_plan.getTasks().setValue( id, field.ordinal(), value, true );
    if ( problem != null )
      Utils.trace( "Task " + id + " " + field + ": " + problem );
  }

  /****************************************** readCount ******************************************/
  private int readCount() throws IOException
  {
    // read the count attribute from the current section's start element, which must be present and non-negative
    String value = m_xml.getAttributeValue( null, XmlLabels.XML_COUNT );
    if ( value == null )
      throw new IOException( "Missing '" + XmlLabels.XML_COUNT + "' attribute on '" + m_xml.getLocalName() + "'" );

    int count = Integer.parseInt( value );
    if ( count < 0 )
      throw new IOException( "Invalid count: " + count );
    return count;
  }

  /***************************************** finishLoad ******************************************/
  private void finishLoad()
  {
    // resolve the default calendar by name, if one was specified in the plan-data attributes
    for ( var calendar : m_plan.getCalendars() )
      if ( calendar.getName().equals( m_calendarName ) )
      {
        m_plan.setDefaultCalendar( calendar );
        return;
      }

    if ( !m_plan.getCalendars().isEmpty() )
    {
      // named calendar wasn't found (missing, renamed, or file never had one) - fall back rather than fail the whole load
      Utils.trace( "Default calendar '" + m_calendarName + "' not found; using calendar 0" );
      m_plan.setDefaultCalendar( m_plan.getCalendar( 0 ) );
    }
    else
      // calendar-data section was absent or empty - plan has no calendars at all
      Utils.trace( "No calendars loaded" );
  }

  /************************************** skipCurrentElement *************************************/
  private void skipCurrentElement() throws Exception
  {
    // only skip if the reader is positioned on a start element, otherwise do nothing
    if ( m_xml.isStartElement() )
      skipElement( m_xml.getLocalName() );
  }

  /***************************************** skipElement *****************************************/
  private void skipElement( String element ) throws Exception
  {
    // consume the remainder of the current element, including nested elements
    int depth = 1;
    while ( depth > 0 && m_xml.hasNext() )
    {
      m_xml.next();

      // each nested start element must be matched by its own end element before we're done
      if ( m_xml.isStartElement() )
        depth++;
      else if ( m_xml.isEndElement() )
        depth--;
    }

    if ( depth != 0 )
      throw new IOException( "Failed to find end of element '" + element + "'" );
  }

}