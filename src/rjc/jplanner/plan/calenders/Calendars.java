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

package rjc.jplanner.plan.calenders;

import java.util.ArrayList;

import rjc.jplanner.Main;
import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.days.Day;
import rjc.table.Utils;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/************************** Holds the complete list of plan calendars ****************************/
/*************************************************************************************************/

public class Calendars extends ArrayList<Calendar>
{
  private static final long serialVersionUID = Main.VERSION.hashCode();

  private Plan              m_plan;

  /**************************************** constructor ******************************************/
  public Calendars( Plan plan )
  {
    // initialise private variables
    m_plan = plan;
  }

  /***************************************** initialise ******************************************/
  public void initialise()
  {
    // these day-types should reflect the default created in Days initialise()
    Day nonWorking = m_plan.getDay( 0 );
    Day working = m_plan.getDay( 1 );
    Day shortDay = m_plan.getDay( 2 );
    Day evening = m_plan.getDay( 3 );
    Day fullTime = m_plan.getDay( 4 );

    // initialise list with default calendars
    clear();
    Date anchor = Date.of( 2000, 1, 1 );
    add( new Calendar( "Standard", anchor, nonWorking, nonWorking, working, working, working, working, working ) );
    getLast().addException( 1, 1, 2026, nonWorking );
    getLast().addException( 3, 4, 2026, nonWorking );
    getLast().addException( 6, 4, 2026, nonWorking );
    getLast().addException( 4, 5, 2026, nonWorking );
    getLast().addException( 25, 5, 2026, nonWorking );
    getLast().addException( 31, 8, 2026, nonWorking );
    getLast().addException( 25, 12, 2026, nonWorking );
    getLast().addException( 28, 12, 2026, nonWorking );

    anchor = Date.of( 2025, 1, 1 );
    add( new Calendar( "Full time", anchor, fullTime ) );

    add( new Calendar( "Fancy", anchor, nonWorking, nonWorking, nonWorking, shortDay, shortDay, evening, evening,
        fullTime, nonWorking, fullTime ) );
    getLast().addException( 25, 12, 2026, nonWorking );
    getLast().addException( 28, 12, 2026, nonWorking );
  }

  /****************************************** setValue *******************************************/
  public String setValue( int calendarIndex, int field, Object newValue, boolean commit )
  {
    // reject duplicate calendar names
    if ( field == Calendar.FIELD.Name.ordinal() )
    {
      String newName = newValue == null ? "" : Utils.clean( newValue.toString() );
      for ( int index = 0; index < size(); index++ )
        if ( index != calendarIndex && newName.equals( get( index ).getName() ) )
          return "Name not unique (clash with calendar " + ( index + 1 ) + ")";
    }

    // delegate to calendar to set value, and return any error message
    return get( calendarIndex ).setValue( field, newValue, commit );
  }

}
