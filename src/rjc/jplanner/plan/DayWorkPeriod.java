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
import rjc.table.data.types.Time;

/*************************************************************************************************/
/**************************** Single working period within a day type ****************************/
/*************************************************************************************************/

public class DayWorkPeriod
{
  public Time m_start; // work period start time
  public Time m_end;   // work period end time

  /**************************************** constructor ******************************************/
  public DayWorkPeriod( double startHour, double endHour )
  {
    // construct work period from from start and end hour points
    if ( startHour >= endHour )
      throw new IllegalArgumentException( "startHour " + startHour + " >= endHour " + endHour );

    m_start = Time.fromHours( startHour );
    m_end = Time.fromHours( endHour );
  }

  /***************************************** toString ********************************************/
  @Override
  public String toString()
  {
    // return period information
    return Utils.name( this ) + "[" + m_start + ", " + m_end + "]";
  }
}