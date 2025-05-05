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

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/*************************************************************************************************/
/************************ Control for displaying & editing of plan notes *************************/
/*************************************************************************************************/

public class PlanNotes extends VBox
{
  private TextArea m_notes = new TextArea();

  /**************************************** constructor ******************************************/
  public PlanNotes()
  {
    // setup notes panel
    setPadding( new Insets( 5.0 ) );
    setSpacing( 5.0 );
    setMinWidth( 0.0 );

    // notes area
    m_notes.setWrapText( true );
    getChildren().addAll( new Label( "Notes" ), m_notes );

    // notes area should grow to fill all available space
    setVgrow( m_notes, Priority.ALWAYS );
  }

  /****************************************** getText ********************************************/
  public String getText()
  {
    // return notes text
    return m_notes.getText();
  }

  /****************************************** setText ********************************************/
  public void setText( String txt )
  {
    // set notes text
    m_notes.setText( txt );
  }

}
