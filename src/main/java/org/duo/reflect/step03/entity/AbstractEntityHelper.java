package org.duo.reflect.step03.entity;

import java.sql.ResultSet;

public abstract class AbstractEntityHelper {

    public abstract Object create(ResultSet rs);
}
