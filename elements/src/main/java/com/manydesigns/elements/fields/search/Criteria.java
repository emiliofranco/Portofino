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

package com.manydesigns.elements.fields.search;

import com.manydesigns.elements.reflection.PropertyAccessor;

import java.util.ArrayList;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/
public class Criteria extends ArrayList<Criteria.Criterion> {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    public void eq(PropertyAccessor accessor, Object value) {
        add(new EqCriterion(accessor, value));
    }
    public void ne(PropertyAccessor accessor, Object value) {
        add(new NeCriterion(accessor, value));
    }

    public void between(PropertyAccessor accessor, Object min, Object max) {
        add(new BetweenCriterion(accessor, min, max));
    }

    public void gt(PropertyAccessor accessor, Object value) {
        add(new GtCriterion(accessor, value));
    }
    public void ge(PropertyAccessor accessor, Object value) {
        add(new GeCriterion(accessor, value));
    }

    public void lt(PropertyAccessor accessor, Object value) {
        add(new LtCriterion(accessor, value));

    }
    public void le(PropertyAccessor accessor, Object value) {
        add(new LeCriterion(accessor, value));
    }

    public void like(PropertyAccessor accessor, String value,
                     TextMatchMode textMatchMode) {
        add(new LikeCriterion(accessor, value, textMatchMode));
    }

    public void ilike(PropertyAccessor accessor, String value,
                     TextMatchMode textMatchMode) {
        add(new IlikeCriterion(accessor, value, textMatchMode));
    }

    public static interface Criterion {
        public PropertyAccessor getPropertyAccessor();
    }

    public static abstract class AbstractCriterion implements Criterion {
        protected final PropertyAccessor accessor;

        public AbstractCriterion(PropertyAccessor accessor) {
            this.accessor = accessor;
        }

        public PropertyAccessor getPropertyAccessor() {
            return accessor;
        }
    }

    public static class EqCriterion extends AbstractCriterion {
        protected final Object value;

        public EqCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class NeCriterion extends AbstractCriterion {
        protected final Object value;

        public NeCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class BetweenCriterion extends AbstractCriterion {
        protected final Object min;
        protected final Object max;

        public BetweenCriterion(PropertyAccessor accessor,
                                Object min, Object max) {
            super(accessor);
            this.min = min;
            this.max = max;
        }

        public Object getMin() {
            return min;
        }

        public Object getMax() {
            return max;
        }
    }

    public static class GtCriterion extends AbstractCriterion {
        protected final Object value;

        public GtCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class GeCriterion extends AbstractCriterion {
        protected final Object value;

        public GeCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class LtCriterion extends AbstractCriterion {
        protected final Object value;

        public LtCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class LeCriterion extends AbstractCriterion {
        protected final Object value;

        public LeCriterion(PropertyAccessor accessor, Object value) {
            super(accessor);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class LikeCriterion extends AbstractCriterion {
        protected final Object value;
        protected final TextMatchMode textMatchMode;

        public LikeCriterion(PropertyAccessor accessor, Object value,
                             TextMatchMode textMatchMode) {
            super(accessor);
            this.value = value;
            this.textMatchMode = textMatchMode;
        }

        public Object getValue() {
            return value;
        }

        public TextMatchMode getTextMatchMode() {
            return textMatchMode;
        }
    }

    public static class IlikeCriterion extends AbstractCriterion {
        protected final Object value;
        protected final TextMatchMode textMatchMode;

        public IlikeCriterion(PropertyAccessor accessor, Object value,
                              TextMatchMode textMatchMode) {
            super(accessor);
            this.value = value;
            this.textMatchMode = textMatchMode;
        }

        public Object getValue() {
            return value;
        }

        public TextMatchMode getTextMatchMode() {
            return textMatchMode;
        }
    }
}
