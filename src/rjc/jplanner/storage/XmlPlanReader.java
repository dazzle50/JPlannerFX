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

import javax.xml.stream.XMLStreamReader;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.days.Day;
import rjc.jplanner.plan.days.DayWorkPeriod;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************************* Reading plan data from XML files ********************************/
/*************************************************************************************************/

public final class XmlPlanReader
{
  private Plan            m_plan;
  private XMLStreamReader m_xml;
  private String          m_calendarName;

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

    processPlanAttributes();

    // process known sections, skip unknown sections, and continue after section failures
    while ( m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isEndElement() && m_xml.getLocalName().equals( XmlLabels.XML_PLAN_DATA ) )
      {
        finishLoad();
        return m_plan;
      }

      if ( !m_xml.isStartElement() )
        continue;

      String section = m_xml.getLocalName();

      try
      {
        switch ( section )
        {
          case XmlLabels.XML_DAY_DATA -> processDays();
          case XmlLabels.XML_CAL_DATA -> processCalendars();
          case XmlLabels.XML_RES_DATA -> processResources();
          case XmlLabels.XML_TASK_DATA -> processTasks();

          default -> {
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

    throw new IOException( "Failed to find end of element '" + XmlLabels.XML_PLAN_DATA + "'" );
  }

  /*********************************** processPlanAttributes *************************************/
  private void processPlanAttributes()
  {
    // stop processing plan attributes after the first invalid known value
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
        Utils.trace( "Unhandled element '" + element + "' inside '" + XmlLabels.XML_DAY_DATA + "'" );
        skipElement( element );
      }
    }

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
        case XmlLabels.XML_ID -> Integer.parseInt( value );
        case XmlLabels.XML_NAME -> name = value;
        case XmlLabels.XML_WORK -> work = Double.parseDouble( value );
        default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
      }
    }

    // collect work periods contained by the current day definition
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
    // calendar loading remains to be implemented; consume the complete section for now
    Utils.trace( "Calendar loading NOT YET IMPLEMENTED" );
    skipElement( XmlLabels.XML_CAL_DATA );
  }

  /************************************** processResources ***************************************/
  private void processResources() throws Exception
  {
    // resource loading remains to be implemented; consume the complete section for now
    Utils.trace( "Resource loading NOT YET IMPLEMENTED" );
    skipElement( XmlLabels.XML_RES_DATA );
  }

  /**************************************** processTasks *****************************************/
  private void processTasks() throws Exception
  {
    // task loading remains to be implemented; consume the complete section for now
    Utils.trace( "Task loading NOT YET IMPLEMENTED" );
    skipElement( XmlLabels.XML_TASK_DATA );
  }

  /***************************************** finishLoad ******************************************/
  private void finishLoad()
  {
    // default calendar can be resolved here after calendar loading is implemented
    if ( m_calendarName != null )
      Utils.trace( "Default calendar not yet resolved: " + m_calendarName );
  }

  /***************************************** skipElement *****************************************/
  private void skipElement( String element ) throws Exception
  {
    // consume the remainder of the current element, including nested elements
    int depth = 1;

    while ( depth > 0 && m_xml.hasNext() )
    {
      m_xml.next();

      if ( m_xml.isStartElement() )
        depth++;
      else if ( m_xml.isEndElement() )
        depth--;
    }

    if ( depth != 0 )
      throw new IOException( "Failed to find end of element '" + element + "'" );
  }

}
