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
  private static final long   serialVersionUID      = Main.VERSION.hashCode();

  private static final int    INITIALS              = FIELD.Initials.ordinal();
  private static final int    NAME                  = FIELD.Name.ordinal();
  private static final int    ORG                   = FIELD.Organisation.ordinal();
  private static final int    GROUP                 = FIELD.Group.ordinal();
  private static final int    ROLE                  = FIELD.Role.ordinal();
  private static final int    ALIAS                 = FIELD.Alias.ordinal();
  private static final int    START                 = FIELD.Start.ordinal();
  private static final int    END                   = FIELD.End.ordinal();
  private static final int    CALENDAR              = FIELD.Calendar.ordinal();
  private static final int    AVAILABLE             = FIELD.Available.ordinal();

  private static final int[]  INITIALS_CLASH_FIELDS = { NAME, ORG, GROUP, ROLE, ALIAS };

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
    // initialise list with default resources
    clear();
    for ( int count = 0; count <= 5; count++ )
      add( new Resource() );

    // set resource 0 to be the special 'unassigned' resource
    get( 0 ).setValue( INITIALS, "[UNASSIGNED]", true );
  }

  /*************************************** getNotNullCount ***************************************/
  public int getNotNullCount()
  {
    // return number of not-null resources in plan (skipping special resource 0)
    int count = 0;
    for ( int index = 1; index < size(); index++ )
      if ( !get( index ).isBlank() )
        count++;

    return count;
  }

  /****************************************** setValue *******************************************/
  public String setValue( int resourceIndex, int field, Object newValue, boolean commit )
  {
    String cleanValue = newValue == null ? "" : Utils.clean( newValue.toString() );

    // reject values that clash with existing initials
    if ( isInitialsClashField( field ) )
    {
      String clash = findClash( cleanValue, INITIALS, -1, "Value clashes with the Initials of resource " );
      if ( clash != null )
        return clash;
    }

    // reject duplicate initials or initials that clash with existing resource text fields
    if ( field == INITIALS )
    {
      String clash = findClash( cleanValue, INITIALS, resourceIndex, "Initials are already used by resource " );
      if ( clash != null )
        return clash;

      for ( int clashField : INITIALS_CLASH_FIELDS )
      {
        clash = findClash( cleanValue, clashField, resourceIndex,
            "Initials clash with the " + FIELD.values()[clashField] + " of resource " );
        if ( clash != null )
          return clash;
      }

      if ( commit )
      {
        Resource resource = get( resourceIndex );
        String oldInitials = resource.getInitials();

        if ( resource.setValue( field, newValue, false ) == null )
        {
          if ( newValue == null )
          {
            // when clearing initials, also clear start, end, and calendar
            resource.setValue( START, null, true );
            resource.setValue( END, null, true );
            resource.setValue( CALENDAR, null, true );
          }
          else if ( oldInitials == null )
          {
            // when setting initials from null, also set default calendar and availability
            Plan plan = m_plan.get();
            resource.setValue( CALENDAR, plan.getDefaultCalendar(), true );
            resource.setValue( AVAILABLE, 1.0, true );
          }
        }
      }
    }

    // delegate to resource to set value, and return any error message
    return get( resourceIndex ).setValue( field, newValue, commit );
  }

  /****************************************** findClash ******************************************/
  private String findClash( String value, int field, int resourceIndex, String message )
  {
    // return message + index if value clashes with value of field in resource at index
    for ( int index = 0; index < size(); index++ )
      if ( index != resourceIndex && value.equals( get( index ).getValue( field ) ) )
        return message + index;

    return null;
  }

  /************************************ isInitialsClashField *************************************/
  private static boolean isInitialsClashField( int field )
  {
    // return true if field must not clash with initials
    for ( int clashField : INITIALS_CLASH_FIELDS )
      if ( field == clashField )
        return true;

    return false;
  }

}