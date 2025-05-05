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

package rjc.jplanner.gui.plan;

import javafx.scene.control.Tab;
import rjc.jplanner.gui.XSplitPane;

/*************************************************************************************************/
/************************* Tab for overall plan control and information **************************/
/*************************************************************************************************/

public class PlanTab extends Tab
{
  private PlanNotes      m_notes      = new PlanNotes();
  private PlanProperties m_properties = new PlanProperties();

  /**************************************** constructor ******************************************/
  public PlanTab()
  {
    // construct tab
    super( "Plan" );
    setClosable( false );

    XSplitPane split = new XSplitPane( m_properties, m_notes );
    split.setPreferredDividerPosition( 300 );

    // only have tab contents set if tab selected
    selectedProperty().addListener( ( property, oldValue, newValue ) ->
    {
      if ( newValue )
        setContent( split );
      else
        setContent( null );
    } );
  }

  /**************************************** getPlanNotes *****************************************/
  public PlanNotes getPlanNotes()
  {
    return m_notes;
  }

  /************************************** getPlanProperties **************************************/
  public PlanProperties getPlanProperties()
  {
    return m_properties;
  }
}
