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
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/************************************* Single plan resource **************************************/
/*************************************************************************************************/

public class Resource
{
  private String   m_initials;  // must be unique across all resources in model
  private String   m_name;      // free text
  private String   m_org;       // free text
  private String   m_group;     // free text
  private String   m_role;      // free text
  private String   m_alias;     // free text
  private Date     m_start;     // date availability starts inclusive
  private Date     m_end;       // date availability end inclusive
  private double   m_available; // number available
  private double   m_cost;      // cost TODO
  private Calendar m_calendar;  // calendar for resource
  private String   m_comment;   // free text

  public enum FIELD
  {
    Initials, Name, Organisation, Group, Role, Alias, Start, End, Available, Cost, Calendar, Comment, MAX
  }

  /**************************************** constructor ******************************************/
  public Resource()
  {
    // nothing needs doing
  }

  /**************************************** toStringShort ****************************************/
  public String toStringShort()
  {
    // short string summary
    return Utils.name( this ) + "[" + m_initials + "]";
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // string summary
    return Utils.name( this ) + "[" + m_initials + ", " + m_name + ", " + m_org + ", " + m_group + ", " + m_role + ", "
        + m_alias + ", " + m_start + ", " + m_end + ", " + m_available + "]";
  }

  /***************************************** getInitials *****************************************/
  public String getInitials()
  {
    return m_initials;
  }

  /****************************************** getStart *******************************************/
  public DateTime getStart()
  {
    // return date-time when this resource starts being available
    if ( m_start == null )
      return DateTime.MIN_VALUE;
    return new DateTime( m_start, Time.MIN_VALUE );
  }

  /******************************************* getEnd ********************************************/
  public DateTime getEnd()
  {
    // return date-time when this resource stops being available
    if ( m_end == null )
      return DateTime.MAX_VALUE;
    return new DateTime( m_end, Time.MAX_VALUE );
  }

  /***************************************** getCalendar *****************************************/
  public Calendar getCalendar()
  {
    // return resource calendar
    return m_calendar;
  }

  /**************************************** getAvailable *****************************************/
  public double getAvailable()
  {
    // return the number of these resource's available
    return m_available;
  }

  /******************************************* getValue ******************************************/
  public Object getValue( int field )
  {
    // return value for the different fields
    switch ( FIELD.values()[field] )
    {
      case Initials:
        return m_initials;
      case Name:
        return m_name;
      case Organisation:
        return m_org;
      case Group:
        return m_group;
      case Role:
        return m_role;
      case Alias:
        return m_alias;
      case Start:
        return m_start;
      case End:
        return m_end;
      case Available:
        return m_available;
      case Calendar:
        return m_calendar == null ? null : m_calendar.getName();
      case Comment:
        return m_comment;
      case Cost:
        return m_cost;
      default:
        throw new IllegalArgumentException( "Unrecognised field " + field );
    }
  }

  /******************************************* isBlank *******************************************/
  public boolean isBlank()
  {
    // resource record is blank if initials are null
    return m_initials == null;
  }

  /******************************************** reset ********************************************/
  private void reset()
  {
    // prepare blank resource when initials are first set
    m_available = 1.0;
    m_calendar = Plan.getCalendar( 0 );
  }

  /****************************************** setValue *******************************************/
  public String setValue( int field, Object newValue, Boolean commit )
  {
    // set/check field value and return null if successful/possible
    switch ( FIELD.values()[field] )
    {
      case Initials:
        // new value can be of any type
        String newInitials = newValue == null ? "" : Utils.clean( newValue.toString() );
        String problem = initialsValidity( newInitials );
        if ( problem != null )
          return problem;
        if ( commit )
        {
          if ( m_initials == null )
            reset();
          m_initials = newInitials;
        }
        return null;

      case Alias:
        // new value can be of any type
        String newAlias = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newAlias.length() > 50 )
          return "Alias too long (max 50 characters)";
        if ( commit )
          m_alias = newAlias;
        return null;

      case Group:
        // new value can be of any type
        String newGroup = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newGroup.length() > 50 )
          return "Group too long (max 50 characters)";
        if ( commit )
          m_group = newGroup;
        return null;

      case Name:
        // new value can be of any type
        String newName = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newName.length() > 50 )
          return "Name too long (max 50 characters)";
        if ( commit )
          m_name = newName;
        return null;

      case Organisation:
        // new value can be of any type
        String newOrg = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newOrg.length() > 50 )
          return "Organisation too long (max 50 characters)";
        if ( commit )
          m_org = newOrg;
        return null;

      case Role:
        // new value can be of any type
        String newRole = newValue == null ? "" : Utils.clean( newValue.toString() );
        if ( newRole.length() > 50 )
          return "Role too long (max 50 characters)";
        if ( commit )
          m_role = newRole;
        return null;

      case Start:
        // check new value is date
        if ( newValue instanceof Date date )
        {
          if ( m_end != null && m_end.getEpochday() < date.getEpochday() )
            return "Start must be before or equal to end date";
          if ( commit )
            m_start = date;
          return null;
        }
        return "Not Date: " + Utils.objectsString( newValue );

      case End:
        // check new value is date
        if ( newValue instanceof Date date )
        {
          if ( m_start != null && m_start.getEpochday() > date.getEpochday() )
            return "End must be after or equal to start date";
          if ( commit )
            m_end = date;
          return null;
        }
        return "Not Date: " + Utils.objectsString( newValue );

      case Available:
        // check new value is double and in range
        if ( newValue instanceof Double avail )
        {
          if ( avail < 0 || avail > 99999.99 )
            return "Value not between 0 and 99999.99";
          if ( commit )
            m_available = avail;
          return null;
        }
        return "Not float: " + Utils.objectsString( newValue );

      case Comment:
        // new value can be of any type
        if ( commit )
          m_comment = newValue == null ? null : newValue.toString();
        return null;

      case Calendar:
        Calendar calendar = Plan.getCalendars().findByName( newValue );
        if ( calendar == null )
          return "Not calendar: " + Utils.objectsString( newValue );
        if ( commit )
          m_calendar = calendar;
        return null;

      case Cost:
      default:
        return "Not implemented";
    }
  }

  /************************************* initialsValidity ****************************************/
  private String initialsValidity( String newInitials )
  {
    // check initials are not too short or long
    if ( newInitials.length() < 1 || newInitials.length() > 20 )
      return "Initials length not between 1 and 20 characters";

    // check name is not a duplicate
    for ( int index = 0; index < Plan.getResources().size(); index++ )
      if ( Plan.getResource( index ) != this && newInitials.equals( Plan.getResource( index ).getInitials() ) )
        return "Initials not unique (clash with resource " + index + ")";

    // no problem so return null
    return null;
  }

}
