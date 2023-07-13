/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package orfel;

import edu.cmu.graphchi.datablocks.BytesToValueConverter;

/**
 *
 * @author Gabriel
 */
public class EdgeTypeConverter implements BytesToValueConverter<EdgeType> {
    public int sizeOf() {
        return 8;
    }

    public EdgeType getValue(byte[] array) {
        int x = ((array[3]  & 0xff) << 24) + ((array[2] & 0xff) << 16) + ((array[1] & 0xff) << 8) + (array[0] & 0xff);
        int y = ((array[7]  & 0xff) << 24) + ((array[6] & 0xff) << 16) + ((array[5] & 0xff) << 8) + (array[4] & 0xff);
        return new EdgeType(x, y);
    }

    public void setValue(byte[] array, EdgeType val) {
        int x = val.year;
        array[3] = (byte) ((x >>> 24) & 0xff);
        array[2] = (byte) ((x >>> 16) & 0xff);
        array[1] = (byte) ((x >>> 8) & 0xff);
        array[0] = (byte) ((x >>> 0) & 0xff);
        int y = val.weight;
        array[7] = (byte) ((y >>> 24) & 0xff);
        array[6] = (byte) ((y >>> 16) & 0xff);
        array[5] = (byte) ((y >>> 8) & 0xff);
        array[4] = (byte) ((y >>> 0) & 0xff);
    }
}
