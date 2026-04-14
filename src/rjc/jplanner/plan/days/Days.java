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

package rjc.jplanner.plan.days;

import java.util.ArrayList;

import rjc.jplanner.Main;
import rjc.table.Utils;

/*************************************************************************************************/
/************************** Holds the complete list of plan day-types ****************************/
/*************************************************************************************************/

public class Days extends ArrayList<Day>
{
  private static final long serialVersionUID = Main.VERSION.hashCode();

  /**************************************** initialise *******************************************/
  public void initialise()
  {
    // initialise list with default day-types, used by Calendars initialise()
    clear();
    add( new Day( "Non working", 0.0 ) );
    add( new Day( "Standard work day", 1.0, 9.0, 13.0, 14.0, 18.0 ) );
    add( new Day( "Morning only", 0.5, 9.0, 13.0 ) );
    add( new Day( "Evening shift", 0.6, 18.0, 22.0 ) );
    add( new Day( "24H day", 1.5, 0.0, 24.0 ) );
  }

  /****************************************** setValue *******************************************/
  public String setValue( int dayIndex, int field, Object newValue, boolean commit )
  {
    // reject duplicate day-type names
    if ( field == Day.FIELD.Name.ordinal() )
    {
      String newName = newValue == null ? "" : Utils.clean( newValue.toString() );
      for ( int index = 0; index < size(); index++ )
        if ( index != dayIndex && get( index ).getName().equals( newName ) )
          return "Name not unique (clash with day-type " + ( index + 1 ) + ")";
    }

    // delegate to day-type to set value, and return any error message
    return get( dayIndex ).setValue( field, newValue, commit );
  }

}
