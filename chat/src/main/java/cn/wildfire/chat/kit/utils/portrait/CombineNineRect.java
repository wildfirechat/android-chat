package cn.wildfire.chat.kit.utils.portrait;

import java.util.LinkedList;
import java.util.List;

public class CombineNineRect {
    /**
     * @param combineWidth
     * @param combineHeight
     * @param count
     * @return
     */
    public static List<CombineBitmapEntity> generateCombineBitmapEntity(
            int combineWidth, int combineHeight, int count) {
        ColumnRowCount mCRC = generateColumnRowCountByCount(count);
        CombineBitmapEntity mBitmapEntity = null;

        float perBitmapWidth = (combineWidth - 1 * 2 * mCRC.columns)
                / mCRC.columns;
        float topDownDelta = combineHeight
                - (mCRC.rows * (perBitmapWidth + CombineBitmapEntity.divide * 2));
        List<CombineBitmapEntity> mList = new LinkedList<CombineBitmapEntity>();
        for (int row = 0; row < mCRC.rows; row++) {
            for (int column = 0; column < mCRC.columns; column++) {

                mBitmapEntity = new CombineBitmapEntity();
                mBitmapEntity.y = 1 + topDownDelta / 2 + row * 2 + row
                        * perBitmapWidth;
                mBitmapEntity.x = 1 + column * 2 + column * perBitmapWidth;
                mBitmapEntity.width = mBitmapEntity.height = perBitmapWidth;
                mList.add(mBitmapEntity);
            }
        }

        switch (count) {
            case 3:
                mList.remove(0);
                modifyListWhenCountThree(mList);
                break;
            case 5:
                mList.remove(0);
                modifyListWhenCountFive(mList);
                break;
            case 7:
                mList.remove(0);
                mList.remove(0);
                modifyListWhenCountSeven(mList);
                break;
            case 8:
                mList.remove(0);
                modifyListWhenCountEight(mList);
                break;
            default:
                break;
        }

        return mList;
    }

    private static void modifyListWhenCountThree(List<CombineBitmapEntity> list) {
        list.get(0).x = (list.get(1).x + list.get(2).x) / 2;
    }

    private static void modifyListWhenCountFive(List<CombineBitmapEntity> list) {
        list.get(0).x = (list.get(3).x + list.get(2).x) / 2;
        list.get(1).x = (list.get(1).x + list.get(3).x) / 2;

    }

    private static void modifyListWhenCountSeven(List<CombineBitmapEntity> list) {
        list.get(0).x = (list.get(1).x + list.get(2).x + list.get(3).x) / 3;
    }

    private static void modifyListWhenCountEight(List<CombineBitmapEntity> list) {
        list.get(0).x = (list.get(2).x + list.get(3).x) / 2;
        list.get(1).x = (list.get(3).x + list.get(4).x) / 2;
    }

    private static ColumnRowCount generateColumnRowCountByCount(int count) {
        switch (count) {
            case 2:
                return new ColumnRowCount(1, 2, count);
            case 3:
            case 4:
                return new ColumnRowCount(2, 2, count);
            case 5:
            case 6:
                return new ColumnRowCount(2, 3, count);
            case 7:
            case 8:
            case 9:
                return new ColumnRowCount(3, 3, count);
            default:
                return new ColumnRowCount(1, 1, count);
        }
    }

    private static class ColumnRowCount {
        int rows;
        int columns;
        int count;

        public ColumnRowCount(int rows, int column, int count) {
            this.rows = rows;
            this.columns = column;
            this.count = count;
        }

        @Override
        public String toString() {
            return "ColumnRowCount [rows=" + rows + ", columns=" + columns
                    + ", count=" + count + "]";
        }
    }
}

