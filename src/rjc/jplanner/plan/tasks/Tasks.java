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

package rjc.jplanner.plan.tasks;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import rjc.jplanner.Main;
import rjc.jplanner.plan.Plan;

/*************************************************************************************************/
/**************************** Holds the complete list of plan tasks ******************************/
/*************************************************************************************************/

public class Tasks extends ArrayList<Task>
{
  private static final long   serialVersionUID = Main.VERSION.hashCode();

  private WeakReference<Plan> m_weakPlan;

  /**************************************** constructor ******************************************/
  public Tasks( Plan plan )
  {
    // hold plan weakly so does not prevent garbage collection of plan
    m_weakPlan = new WeakReference<>( plan );
  }

  /****************************************** initialise *****************************************/
  public void initialise()
  {
    // initialise list with default tasks (including special task 0 which is overall project summary)
    clear();
    for ( int count = 0; count <= 10; count++ )
      add( new Task() );

    setupTaskZero();
  }

  /**************************************** setupTaskZero ****************************************/
  public void setupTaskZero()
  {
    // setup special task 0
    get( 0 ).setValue( Task.FIELD.Title.ordinal(), "[OVERALL_PROJECT]", true );
  }

  /*************************************** getNotNullCount ***************************************/
  public int getNotNullCount()
  {
    // return number of not-null tasks in plan (skipping special task 0)
    int count = 0;
    for ( int id = 1; id < size(); id++ )
      if ( !get( id ).isBlank() )
        count++;

    return count;
  }

  /****************************************** setValue *******************************************/
  public String setValue( int taskIndex, int field, Object newValue, boolean commit )
  {
    if ( newValue != null )
    {
      try
      {
        // validate and parse specific fields if required
        if ( field == Task.FIELD.Predecessors.ordinal() )
          newValue = Predecessors.parse( newValue.toString(), this );
        else if ( field == Task.FIELD.Resources.ordinal() )
          newValue = TaskResources.parse( newValue.toString(), m_weakPlan.get().getResources() );
      }
      catch ( IllegalArgumentException exception )
      {
        // validation failed, return error message
        return exception.getMessage();
      }
    }

    // delegate to task to set value, and return any error message
    return get( taskIndex ).setValue( field, newValue, commit );
  }

}
