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

import java.util.ArrayList;

import rjc.jplanner.Main;
import rjc.table.data.types.Date;

/*************************************************************************************************/
/************************** Holds the complete list of plan calendars ****************************/
/*************************************************************************************************/

public class Calendars extends ArrayList<Calendar>
{
  private static final long serialVersionUID = Main.VERSION.hashCode();

  /***************************************** initialise ******************************************/
  public void initialise()
  {
    // these day-types should reflect the default created in Days initialise()
    Day nonWorking = Plan.getDay( 0 );
    Day working = Plan.getDay( 1 );
    Day shortDay = Plan.getDay( 2 );
    Day evening = Plan.getDay( 3 );
    Day fullTime = Plan.getDay( 4 );

    // initialise list with default calendars
    clear();
    Date anchor = new Date( 2000, 1, 1 );
    add( new Calendar( "Standard", anchor, nonWorking, nonWorking, working, working, working, working, working ) );
    getLast().addException( 5, 5, 2025, nonWorking );
    getLast().addException( 26, 5, 2025, nonWorking );
    getLast().addException( 25, 8, 2025, nonWorking );
    getLast().addException( 25, 12, 2025, nonWorking );
    getLast().addException( 26, 12, 2025, nonWorking );
    getLast().addException( 1, 1, 2026, nonWorking );
    getLast().addException( 3, 4, 2026, nonWorking );
    getLast().addException( 6, 4, 2026, nonWorking );

    anchor = new Date( 2025, 1, 1 );
    add( new Calendar( "Full time", anchor, fullTime ) );

    add( new Calendar( "Fancy", anchor, nonWorking, nonWorking, nonWorking, shortDay, shortDay, evening, evening,
        fullTime, nonWorking, fullTime ) );
    getLast().addException( 25, 12, 2025, nonWorking );
    getLast().addException( 26, 12, 2025, nonWorking );
  }

  /**************************************** getNameArray *****************************************/
  public Object[] getNameArray()
  {
    // return array of calendar names
    var names = new String[size()];
    for ( int index = 0; index < size(); index++ )
      names[index] = get( index ).getName();

    return names;
  }

}
