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

package rjc.jplanner.plan.resources;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import rjc.jplanner.Main;
import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.resources.Resource.FIELD;
import rjc.table.Utils;

/*************************************************************************************************/
/************************** Holds the complete list of plan resources ****************************/
/*************************************************************************************************/

public class Resources extends ArrayList<Resource>
{
  private static final long   serialVersionUID = Main.VERSION.hashCode();

  private WeakReference<Plan> m_plan;

  /**************************************** constructor ******************************************/
  public Resources( Plan plan )
  {
    // hold plan weakly so does not prevent garbage collection of plan
    m_plan = new WeakReference<>( plan );
  }

  /***************************************** initialise ******************************************/
  public void initialise()
  {
    // initialise list with default resources (including resource 0 the special 'unassigned' resource)
    clear();
    for ( int count = 0; count <= 5; count++ )
      add( new Resource() );
  }

  /*************************************** getNotNullCount ***************************************/
  public int getNotNullCount()
  {
    // return number of not-null resources in plan (skipping special resource 0)
    int count = 0;
    for ( int id = 1; id < size(); id++ )
      if ( !get( id ).isBlank() )
        count++;

    return count;
  }

  /****************************************** setValue *******************************************/
  public String setValue( int resourceIndex, int field, Object newValue, boolean commit )
  {
    // reject duplicate resource initials
    if ( field == FIELD.Initials.ordinal() )
    {
      String newInitials = newValue == null ? "" : Utils.clean( newValue.toString() );
      for ( int index = 0; index < size(); index++ )
        if ( index != resourceIndex && newInitials.equals( get( index ).getInitials() ) )
          return "Initials not unique (clash with resource " + ( index + 1 ) + ")";

      if ( commit && get( resourceIndex ).setValue( field, newValue, false ) == null )
      {
        if ( newValue == null )
        {
          // when clearing initials, also clear start + end + calendar
          get( resourceIndex ).setValue( FIELD.Start.ordinal(), null, true );
          get( resourceIndex ).setValue( FIELD.End.ordinal(), null, true );
          get( resourceIndex ).setValue( FIELD.Calendar.ordinal(), null, true );
        }
        else
        {
          // when setting initials from null, also set default calendar and available to 1.0
          var oldInitials = get( resourceIndex ).getInitials();
          if ( oldInitials == null )
          {
            var defaultCal = m_plan.get().getDefaultCalendar();
            get( resourceIndex ).setValue( FIELD.Calendar.ordinal(), defaultCal, true );
            get( resourceIndex ).setValue( FIELD.Available.ordinal(), 1.0, true );
          }
        }
      }
    }

    // delegate to resource to set value, and return any error message
    return get( resourceIndex ).setValue( field, newValue, commit );
  }

}
