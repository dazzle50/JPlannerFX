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
  private String   m_initials;     // must be unique across all resources in model
  private String   m_name;         // free text
  private String   m_org;          // free text
  private String   m_group;        // free text
  private String   m_role;         // free text
  private String   m_alias;        // free text
  private Date     m_start;        // date availability starts inclusive
  private Date     m_end;          // date availability end inclusive
  private double   m_availability; // number available
  private double   m_cost;         // cost TODO
  private Calendar m_calendar;     // calendar for resource
  private String   m_comment;      // free text

  /**************************************** constructor ******************************************/
  public Resource()
  {
    // initialise private variables
    m_availability = 1.0;
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
        + m_alias + ", " + m_start + ", " + m_end + ", " + m_availability + "]";
  }

}
