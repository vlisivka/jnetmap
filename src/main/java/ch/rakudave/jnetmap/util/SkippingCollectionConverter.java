package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.*;

/**
 * Custom version of the CollectionConverter, will not fail if the class of an entry is missing
 *
 * @author rakudave
 */
@SuppressWarnings("unchecked")
public class SkippingCollectionConverter extends AbstractCollectionConverter {

    public SkippingCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    @SuppressWarnings("rawtypes")
    public boolean canConvert(Class type) {
        return type.equals(ArrayList.class) || type.equals(HashSet.class) || type.equals(LinkedList.class) || type.equals(Vector.class)
                || type.getName().equals("java.util.LinkedHashSet");
    }

    @SuppressWarnings("rawtypes")
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Collection collection = (Collection) source;
        for (Object item : collection) {
            writeItem(item, context, writer);
        }
    }

    @SuppressWarnings("rawtypes")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Collection collection = (Collection) createCollection(context.getRequiredType());
        populateCollection(reader, context, collection);
        return collection;
    }

    @SuppressWarnings("rawtypes")
    protected void populateCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            try {
                Object item = readItem(reader, context, collection);
                collection.add(item);
            } catch (Exception e) {
                Logger.debug("Class not found, item will be ignored", e);
            }
            reader.moveUp();
        }
    }

}
