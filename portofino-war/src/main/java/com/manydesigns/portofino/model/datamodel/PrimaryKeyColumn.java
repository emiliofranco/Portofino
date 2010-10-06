/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.model.datamodel;

import com.manydesigns.elements.logging.LogUtil;

import java.util.logging.Logger;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/
public class PrimaryKeyColumn {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************
    protected final PrimaryKey pk;
    protected String columnName;
    protected Column column;

    public static final Logger logger =
            LogUtil.getLogger(PrimaryKeyColumn.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public PrimaryKeyColumn(PrimaryKey pk, String columnName) {
        this.pk = pk;
        this.columnName = columnName;
    }

    public void init() {
        column = pk.getTable().findColumnByName(columnName);
        if (column == null) {
            LogUtil.warningMF(logger,
                    "Cannor wire primary key column ''{0}'' to primary key ''{1}''",
                    columnName, pk);

        }
    }

    //**************************************************************************
    // Getters/setter
    //**************************************************************************

    public PrimaryKey getPk() {
        return pk;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Column getColumn() {
        return column;
    }
}
