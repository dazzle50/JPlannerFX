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
    // initialise private variables
    m_available = 1.0;
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

  /******************************************* getValue ******************************************/
  public Object getValue( int field )
  {
    // return value for the different fields
    switch ( FIELD.values()[field] )
    {
      case Initials:
        return m_initials;
      case Alias:
        return m_alias;
      case Available:
        return m_available;
      case Calendar:
        return m_calendar;
      case Comment:
        return m_comment;
      case Cost:
        return m_cost;
      case Group:
        return m_group;
      case Name:
        return m_name;
      case Organisation:
        return m_org;
      case Role:
        return m_role;
      case Start:
        return m_start;
      case End:
        return m_end;
      default:
        throw new IllegalArgumentException( "Unrecognised field " + field );
    }
  }

  /******************************************** isBlank ******************************************/
  public boolean isBlank()
  {
    // resource record is blank if initials are null
    return m_initials == null;
  }

  /****************************************** setValue *******************************************/
  public String setValue( int field, Object newValue, Boolean commit )
  {
    // set/check field value and return null if successful/possible
    switch ( FIELD.values()[field] )
    {
      case Initials:
        // new value can be of any type
        if ( commit )
          m_initials = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Alias:
        // new value can be of any type
        if ( commit )
          m_alias = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Group:
        // new value can be of any type
        if ( commit )
          m_group = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Name:
        // new value can be of any type
        if ( commit )
          m_name = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Organisation:
        // new value can be of any type
        if ( commit )
          m_org = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Role:
        // new value can be of any type
        if ( commit )
          m_role = newValue == null ? null : Utils.clean( newValue.toString() );
        return null;

      case Start:
        // check new value is date
        if ( newValue instanceof Date date )
        {
          if ( commit )
            m_start = date;
          return null;
        }
        return "Not Date: " + Utils.objectsString( newValue );

      case End:
        // check new value is date
        if ( newValue instanceof Date date )
        {
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

      case Cost:
      case Calendar:

      default:
        return "Not implemented";
    }
  }

}
