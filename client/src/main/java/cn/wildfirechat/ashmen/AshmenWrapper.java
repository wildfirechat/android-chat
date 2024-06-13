/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.ashmen;

import android.os.Build;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.SharedMemory;
import android.system.ErrnoException;

import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

import cn.wildfirechat.utils.MemoryFileHelper;
import cn.wildfirechat.utils.MemoryFileUtil;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AshmenWrapper implements Parcelable {
    private SharedMemory sm;

    // 跨进程时，不传输MemoryFile
    private MemoryFile mf;
    private ParcelFileDescriptor pfd;
    private int length;

    private AshmenWrapper() {

    }

    public static AshmenWrapper create(String name, int length) {
        AshmenWrapper ashmenWrapper = new AshmenWrapper();
        ashmenWrapper.length = length;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                ashmenWrapper.sm = SharedMemory.create(name, length);
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                MemoryFile memoryFile = new MemoryFile(name, length);
                FileDescriptor fileDescriptor = MemoryFileUtil.getFileDescriptor(memoryFile);
                ashmenWrapper.pfd = ParcelFileDescriptor.dup(fileDescriptor);
                ashmenWrapper.mf = memoryFile;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ashmenWrapper;
    }

    public void writeBytes(byte[] src, int offset, int count) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                ByteBuffer buffer = sm.mapReadWrite();
                buffer.put(src, offset, count);
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                this.mf.writeBytes(src, offset, 0, count);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public int readBytes(byte[] dst, int offset, int count) {
        int len = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                ByteBuffer buffer = this.sm.mapReadOnly();
                buffer.get(dst, offset, count);
                len = count;
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                len = this.mf.readBytes(dst, 0, offset, count);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return len;
    }

    public void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.sm.close();
        } else {
            this.mf.close();
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            dest.writeParcelable(this.sm, flags);
        } else {
            dest.writeParcelable(this.pfd, flags);
        }
    }

    public void readFromParcel(Parcel source) {
        this.length = source.readInt();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.sm = source.readParcelable(SharedMemory.class.getClassLoader());
        } else {
            this.pfd = source.readParcelable(ParcelFileDescriptor.class.getClassLoader());
            this.mf = MemoryFileHelper.openMemoryFile(pfd, length, MemoryFileHelper.OPEN_READWRITE);
        }
    }

    protected AshmenWrapper(Parcel in) {
        this.length = in.readInt();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.sm = in.readParcelable(SharedMemory.class.getClassLoader());
        } else {
            this.pfd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
            this.mf = MemoryFileHelper.openMemoryFile(pfd, length, MemoryFileHelper.OPEN_READWRITE);
        }
    }

    public static final Creator<AshmenWrapper> CREATOR = new Creator<AshmenWrapper>() {
        @Override
        public AshmenWrapper createFromParcel(Parcel source) {
            return new AshmenWrapper(source);
        }

        @Override
        public AshmenWrapper[] newArray(int size) {
            return new AshmenWrapper[size];
        }
    };
}
