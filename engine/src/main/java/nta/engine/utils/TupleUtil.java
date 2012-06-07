package nta.engine.utils;

import nta.catalog.Column;
import nta.catalog.Schema;
import nta.datum.Datum;
import nta.datum.DatumFactory;
import nta.storage.StorageUtil;
import nta.storage.Tuple;
import nta.storage.TupleRange;
import nta.storage.VTuple;

import java.nio.ByteBuffer;

public class TupleUtil {
  public static int[] getTargetIds(Schema inSchema, Schema outSchema) {
    int[] targetIds = new int[outSchema.getColumnNum()];
    int i = 0;
    for (Column target : outSchema.getColumns()) {
      targetIds[i] = inSchema.getColumnId(target.getQualifiedName());
      i++;
    }

    return targetIds;
  }

  public static Tuple project(Tuple in, Tuple out, int[] targetIds) {
    out.clear();
    for (int idx = 0; idx < targetIds.length; idx++) {
      out.put(idx, in.get(targetIds[idx]));
    }
    return out;
  }

  public static byte [] toBytes(Schema schema, Tuple tuple) {
    int size = StorageUtil.getRowByteSize(schema);
    ByteBuffer bb = ByteBuffer.allocate(size);
    Column col;
    for (int i = 0; i < schema.getColumnNum(); i++) {
      col = schema.getColumn(i);
      switch (col.getDataType()) {
        case BYTE: bb.put(tuple.get(i).asByte()); break;
        case CHAR: bb.put(tuple.get(i).asByte()); break;
        case BOOLEAN: bb.put(tuple.get(i).asByte()); break;
        case SHORT: bb.putShort(tuple.get(i).asShort()); break;
        case INT: bb.putInt(tuple.get(i).asInt()); break;
        case LONG: bb.putLong(tuple.get(i).asLong()); break;
        case FLOAT: bb.putFloat(tuple.get(i).asFloat()); break;
        case DOUBLE: bb.putDouble(tuple.get(i).asDouble()); break;
        case STRING:
          byte [] _string = tuple.get(i).asByteArray();
          bb.putInt(_string.length);
          bb.put(_string);
          break;
        case BYTES:
          byte [] bytes = tuple.get(i).asByteArray();
          bb.putInt(bytes.length);
          bb.put(bytes);
          break;
        case IPv4:
          byte [] ipBytes = tuple.getIPv4Bytes(i);
          bb.put(ipBytes);
          break;
        case IPv6: bb.put(tuple.getIPv6Bytes(i)); break;
        default:
      }
    }

    bb.flip();
    byte [] buf = new byte [bb.limit()];
    bb.get(buf);
    return buf;
  }

  public static Tuple toTuple(Schema schema, byte [] bytes) {
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    Tuple tuple = new VTuple(schema.getColumnNum());
    Column col;
    for (int i =0; i < schema.getColumnNum(); i++) {
      col = schema.getColumn(i);

      switch (col.getDataType()) {
        case BYTE: tuple.put(i, DatumFactory.createByte(bb.get())); break;
        case CHAR: tuple.put(i, DatumFactory.createChar(bb.get())); break;
        case BOOLEAN: tuple.put(i, DatumFactory.createBool(bb.get())); break;
        case SHORT: tuple.put(i, DatumFactory.createShort(bb.getShort())); break;
        case INT: tuple.put(i, DatumFactory.createInt(bb.getInt())); break;
        case LONG: tuple.put(i, DatumFactory.createLong(bb.getLong())); break;
        case FLOAT: tuple.put(i, DatumFactory.createFloat(bb.getFloat())); break;
        case DOUBLE: tuple.put(i, DatumFactory.createDouble(bb.getDouble())); break;
        case STRING:
          byte [] _string = new byte[bb.getInt()];
          bb.get(_string);
          tuple.put(i, DatumFactory.createString(new String(_string)));
          break;
        case BYTES:
          byte [] _bytes = new byte[bb.getInt()];
          bb.get(_bytes);
          tuple.put(i, DatumFactory.createBytes(_bytes));
          break;
        case IPv4:
          byte [] _ipv4 = new byte[4];
          bb.get(_ipv4);
          tuple.put(i, DatumFactory.createIPv4(_ipv4));
          break;
        case IPv6:
          // TODO - to be implemented
      }
    }
    return tuple;
  }

  public static TupleRange [] getPartitions(Schema schema, int partNum, Tuple start, Tuple end) {
    boolean splittable = false;

    Column col;
    TupleRange [] ranges = new TupleRange[partNum];

    Datum[] term = new Datum[schema.getColumnNum()];
    Datum[] prevValues = new Datum[schema.getColumnNum()];

    // initialize term and previous values
    for (int i = 0; i < schema.getColumnNum(); i++) {
      col = schema.getColumn(i);
      prevValues[i] = start.get(i);
      switch (col.getDataType()) {
        case CHAR:
          int sChar = start.get(i).asChar();
          int eChar = end.get(i).asChar();
          int rangeChar;
          if ((eChar - sChar) > partNum) {
            rangeChar = (eChar - sChar) / partNum;
          } else {
            rangeChar = 1;
          }
          term[i] = DatumFactory.createInt(rangeChar);
        case BYTE:
          byte sByte = start.get(i).asByte();
          byte eByte = end.get(i).asByte();
          int rangeByte;
          if ((eByte - sByte) > partNum) {
            rangeByte = (eByte - sByte) / partNum;
          } else {
            rangeByte = 1;
          }
          term[i] = DatumFactory.createByte((byte)rangeByte);
          break;

        case SHORT:
          short sShort = start.get(i).asShort();
          short eShort = end.get(i).asShort();
          int rangeShort;
          if ((eShort - sShort) > partNum) {
            rangeShort = (eShort - sShort) / partNum;
          } else {
            rangeShort = 1;
          }
          term[i] = DatumFactory.createShort((short) rangeShort);
          break;

        case INT:
          int sInt = start.get(i).asInt();
          int eInt = end.get(i).asInt();
          int rangeInt;
          if ((eInt - sInt) > partNum) {
            rangeInt = (eInt - sInt) / partNum;
          } else {
            rangeInt = 1;
          }
          term[i] = DatumFactory.createInt(rangeInt);
          break;

        case LONG:
          long sLong = start.get(i).asLong();
          long eLong = end.get(i).asLong();
          long rangeLong;
          if ((eLong - sLong) > partNum) {
            rangeLong = ((eLong - sLong) / partNum);
          } else {
            rangeLong = 1;
          }
          term[i] = DatumFactory.createLong(rangeLong);
          break;

        case FLOAT:
          float sFloat = start.get(i).asFloat();
          float eFloat = end.get(i).asFloat();
          float rangeFloat;
          if ((eFloat - sFloat) > partNum) {
            rangeFloat = ((eFloat - sFloat) / partNum);
          } else {
            rangeFloat = 1;
          }
          term[i] = DatumFactory.createFloat(rangeFloat);
          break;
        case DOUBLE:
          double sDouble = start.get(i).asDouble();
          double eDouble = end.get(i).asDouble();
          double rangeDouble;
          if ((eDouble - sDouble) > partNum) {
            rangeDouble = ((eDouble - sDouble) / partNum);
          } else {
            rangeDouble = 1;
          }
          term[i] = DatumFactory.createDouble(rangeDouble);
          break;
        case STRING:
          throw new UnsupportedOperationException();
        case IPv4:
          throw new UnsupportedOperationException();
        case BYTES:
          throw new UnsupportedOperationException();
        default:
          throw new UnsupportedOperationException();
      }
    }

    for (int p = 0; p < partNum; p++) {
      Tuple sTuple = new VTuple(schema.getColumnNum());
      Tuple eTuple = new VTuple(schema.getColumnNum());
      for (int i = 0; i < schema.getColumnNum(); i++) {
        col = schema.getColumn(i);
        sTuple.put(i, prevValues[i]);
        switch (col.getDataType()) {
          case CHAR:
            char endChar = (char) (prevValues[i].asChar() + term[i].asChar());
            if (endChar > end.get(i).asByte()) {
              eTuple.put(i, end.get(i));
            } else {
              eTuple.put(i, DatumFactory.createChar(endChar));
            }
            prevValues[i] = DatumFactory.createChar(endChar);
            break;
          case BYTE:
            byte endByte = (byte) (prevValues[i].asByte() + term[i].asByte());
            if (endByte > end.get(i).asByte()) {
              eTuple.put(i, end.get(i));
            } else {
              eTuple.put(i, DatumFactory.createByte(endByte));
            }
            prevValues[i] = DatumFactory.createByte(endByte);
            break;
          case SHORT:
            int endShort = (short) (prevValues[i].asShort() + term[i].asShort());
            if (endShort > end.get(i).asShort()) {
              eTuple.put(i, end.get(i));
            } else {
              // TODO - to consider overflow
              eTuple.put(i, DatumFactory.createShort((short) endShort));
            }
            prevValues[i] = DatumFactory.createShort((short) endShort);
            break;
          case INT:
            int endInt = (prevValues[i].asInt() + term[i].asInt());
            if (endInt > end.get(i).asInt()) {
              eTuple.put(i, end.get(i));
            } else {
              // TODO - to consider overflow
              eTuple.put(i, DatumFactory.createInt(endInt));
            }
            prevValues[i] = DatumFactory.createInt(endInt);
            break;

          case LONG:
            long endLong = (prevValues[i].asLong() + term[i].asLong());
            if (endLong > end.get(i).asLong()) {
              eTuple.put(i, end.get(i));
            } else {
              // TODO - to consider overflow
              eTuple.put(i, DatumFactory.createLong(endLong));
            }
            prevValues[i] = DatumFactory.createLong(endLong);
            break;

          case FLOAT:
            float endFloat = (prevValues[i].asFloat() + term[i].asFloat());
            if (endFloat > end.get(i).asInt()) {
              eTuple.put(i, end.get(i));
            } else {
              // TODO - to consider overflow
              eTuple.put(i, DatumFactory.createFloat(endFloat));
            }
            prevValues[i] = DatumFactory.createFloat(endFloat);
            break;
          case DOUBLE:
            double endDouble = (prevValues[i].asDouble() + term[i].asDouble());
            if (endDouble > end.get(i).asDouble()) {
              eTuple.put(i, end.get(i));
            } else {
              // TODO - to consider overflow
              eTuple.put(i, DatumFactory.createDouble(endDouble));
            }
            prevValues[i] = DatumFactory.createDouble(endDouble);
            break;
          case STRING:
            throw new UnsupportedOperationException();
          case IPv4:
            throw new UnsupportedOperationException();
          case BYTES:
            throw new UnsupportedOperationException();
          default:
            throw new UnsupportedOperationException();
        }
      }
      ranges[p] = new TupleRange(sTuple, eTuple);
    }

    return ranges;
  }
}
