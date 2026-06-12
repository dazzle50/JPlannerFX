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

package rjc.jplanner.gui.editors;

import rjc.jplanner.gui.PredecessorsField;
import rjc.table.view.editor.AbstractCellEditor;

/*************************************************************************************************/
/**************************** Table cell editor for task predecessors ****************************/
/*************************************************************************************************/

public class EditorPredecessors extends AbstractCellEditor
{
  PredecessorsField m_editor = new PredecessorsField();

  /**************************************** constructor ******************************************/
  public EditorPredecessors()
  {
    // create predecessors table cell editor
    setControl( m_editor );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor predecessors value
    return m_editor.getText();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set value depending on type
    m_editor.setText( value == null ? "" : value.toString() );
  }

}
