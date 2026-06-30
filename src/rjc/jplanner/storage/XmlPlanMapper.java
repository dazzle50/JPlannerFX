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

import javax.xml.stream.XMLStreamReader;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.resources.Resource;
import rjc.jplanner.plan.tasks.Task;

/*************************************************************************************************/
/************************* Reading and writing of plan data to XML files *************************/
/*************************************************************************************************/

public class XmlPlanMapper
{
  private Plan m_plan;

  /***************************************** constructor ******************************************/
  public XmlPlanMapper( Plan plan )
  {
    m_plan = plan;
  }

  /******************************************** load *********************************************/
  public Plan load( XMLStreamReader xsr )
  {
    // TODO Auto-generated method stub
    return null;
  }

  /******************************************** write ********************************************/
  public void write( XmlWriter xml ) throws Exception
  {
    // write plan to XML stream
    xml.startElement( XmlLabels.XML_PLAN_DATA );
    xml.attribute( XmlLabels.XML_TITLE, m_plan.getTitle() );
    xml.attribute( XmlLabels.XML_START, m_plan.getDefaultStart() );
    xml.attribute( XmlLabels.XML_CALENDAR, m_plan.getDefaultCalendar() );
    xml.attribute( XmlLabels.XML_DT_FORMAT, m_plan.getDateTimeFormat() );
    xml.attribute( XmlLabels.XML_D_FORMAT, m_plan.getDateFormat() );
    xml.attribute( XmlLabels.XML_NOTES, m_plan.getNotes() );

    // write day, calendar, resource, and task data
    writeDayTypes( xml );
    writeCalendars( xml );
    writeResources( xml );
    writeTasks( xml );

    xml.endElement(); // XML_PLAN_DATA
  }

  /**************************************** writeDayTypes ****************************************/
  private void writeDayTypes( XmlWriter xml ) throws Exception
  {
    // write day-type definitions
    var days = m_plan.getDays();
    xml.startElement( XmlLabels.XML_DAY_DATA );

    int id = 0;
    for ( var day : days )
    {
      xml.startElement( XmlLabels.XML_DAY );

      xml.attribute( XmlLabels.XML_ID, id++ );
      xml.attribute( XmlLabels.XML_NAME, day.getName() );
      xml.attribute( XmlLabels.XML_WORK, day.getWork() );

      // write work periods
      int pid = 0;
      for ( var period : day.getWorkPeriods() )
      {
        xml.startElement( XmlLabels.XML_PERIOD );
        xml.attribute( XmlLabels.XML_ID, pid++ );
        xml.attribute( XmlLabels.XML_START, period.m_start );
        xml.attribute( XmlLabels.XML_END, period.m_end );
        xml.endElement(); // end XML_PERIOD
      }

      xml.endElement(); // end XML_DAY
    }

    xml.endElement(); // end XML_DAY_DATA
  }

  /*************************************** writeCalendars ****************************************/
  private void writeCalendars( XmlWriter xml ) throws Exception
  {
    // write calendar definitions
    var calendars = m_plan.getCalendars();
    xml.startElement( XmlLabels.XML_CAL_DATA );

    int id = 0;
    for ( var calendar : calendars )
    {
      xml.startElement( XmlLabels.XML_CALENDAR );

      xml.attribute( XmlLabels.XML_ID, id++ );
      xml.attribute( XmlLabels.XML_NAME, calendar.getName() );
      xml.attribute( XmlLabels.XML_ANCHOR, calendar.getAnchor() );

      // write normal cycle days
      int normalId = 0;
      for ( var normalDay : calendar.getNormals() )
      {
        xml.startElement( XmlLabels.XML_NORMAL );
        xml.attribute( XmlLabels.XML_ID, normalId++ );
        xml.attribute( XmlLabels.XML_DAY, m_plan.getIndex( normalDay ) ); // reference to day ID
        xml.endElement(); // end XML_NORMAL
      }

      // write exceptions
      for ( var exceptionEntry : calendar.getExceptions().entrySet() )
      {
        xml.startElement( XmlLabels.XML_EXCEPTION );
        xml.attribute( XmlLabels.XML_DATE, exceptionEntry.getKey() );
        xml.attribute( XmlLabels.XML_DAY, m_plan.getIndex( exceptionEntry.getValue() ) ); // reference to day ID
        xml.endElement(); // end XML_EXCEPTION
      }

      xml.endElement(); // end XML_CALENDAR
    }

    xml.endElement(); // end XML_CAL_DATA
  }

  /*************************************** writeResources ****************************************/
  private void writeResources( XmlWriter xml ) throws Exception
  {
    // write resource definitions
    var resources = m_plan.getResources();
    xml.startElement( XmlLabels.XML_RES_DATA );
    xml.attribute( XmlLabels.XML_COUNT, resources.size() );

    int id = 0;
    for ( var resource : resources )
    {
      // skip blank resources
      if ( resource.isBlank() )
        continue;

      xml.startElement( XmlLabels.XML_RESOURCE );

      xml.attribute( XmlLabels.XML_ID, id++ );
      xml.attribute( XmlLabels.XML_INITIALS, resource.getInitials() );
      xml.attribute( XmlLabels.XML_NAME, resource.getValue( Resource.FIELD.Name.ordinal() ) );
      xml.attribute( XmlLabels.XML_ORG, resource.getValue( Resource.FIELD.Organisation.ordinal() ) );
      xml.attribute( XmlLabels.XML_GROUP, resource.getValue( Resource.FIELD.Group.ordinal() ) );
      xml.attribute( XmlLabels.XML_ROLE, resource.getValue( Resource.FIELD.Role.ordinal() ) );
      xml.attribute( XmlLabels.XML_ALIAS, resource.getValue( Resource.FIELD.Alias.ordinal() ) );
      xml.attribute( XmlLabels.XML_START, resource.getValue( Resource.FIELD.Start.ordinal() ) );
      xml.attribute( XmlLabels.XML_END, resource.getValue( Resource.FIELD.End.ordinal() ) );
      xml.attribute( XmlLabels.XML_AVAIL, resource.getAvailable() );

      // write calendar reference
      var calendar = resource.getCalendar();
      if ( calendar != null )
        xml.attribute( XmlLabels.XML_CALENDAR, m_plan.getIndex( calendar ) );

      xml.attribute( XmlLabels.XML_COMMENT, resource.getValue( Resource.FIELD.Comment.ordinal() ) );

      xml.endElement(); // end XML_RESOURCE
    }

    xml.endElement(); // end XML_RES_DATA
  }

  /**************************************** writeTasks ******************************************/
  private void writeTasks( XmlWriter xml ) throws Exception
  {
    // write task definitions
    var tasks = m_plan.getTasks();
    xml.startElement( XmlLabels.XML_TASK_DATA );
    xml.attribute( XmlLabels.XML_COUNT, tasks.size() );

    int id = 0;
    for ( var task : tasks )
    {
      // skip blank tasks (but always include special task 0)
      if ( id > 0 && task.isBlank() )
        continue;

      xml.startElement( XmlLabels.XML_TASK );

      xml.attribute( XmlLabels.XML_ID, id++ );
      xml.attribute( XmlLabels.XML_TITLE, task.getValue( Task.FIELD.Title.ordinal() ) );
      xml.attribute( XmlLabels.XML_TYPE, task.getTaskType() );
      xml.attribute( XmlLabels.XML_DURATION, task.getValue( Task.FIELD.Duration.ordinal() ) );
      xml.attribute( XmlLabels.XML_PRIORITY, task.getValue( Task.FIELD.Priority.ordinal() ) );

      // write optional date-time fields
      var start = task.getValue( Task.FIELD.Start.ordinal() );
      if ( start != null )
        xml.attribute( XmlLabels.XML_START, start );

      var end = task.getValue( Task.FIELD.End.ordinal() );
      if ( end != null )
        xml.attribute( XmlLabels.XML_END, end );

      var deadline = task.getValue( Task.FIELD.Deadline.ordinal() );
      if ( deadline != null )
        xml.attribute( XmlLabels.XML_DEADLINE, deadline );

      // write optional string fields
      var predecessors = task.getValue( Task.FIELD.Predecessors.ordinal() );
      if ( predecessors != null )
        xml.attribute( XmlLabels.XML_PREDS, predecessors.toString() );

      var resources = task.getValue( Task.FIELD.Resources.ordinal() );
      if ( resources != null )
        xml.attribute( XmlLabels.XML_RESOURCES, resources.toString() );

      var comment = task.getValue( Task.FIELD.Comment.ordinal() );
      if ( comment != null )
        xml.attribute( XmlLabels.XML_COMMENT, comment );

      xml.endElement(); // end XML_TASK
    }

    xml.endElement(); // end XML_TASK_DATA
  }

}
