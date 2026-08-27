package com.misanthropy.fastchunkgen.rewrites.chunk_serializer.common;

import com.misanthropy.fastchunkgen.base.mixin.access.INbtCompound;
import net.minecraft.nbt.*;

import java.util.Map;

public class NbtWriterVisitor implements TagVisitor {
    private final NbtWriter writer;

    public NbtWriterVisitor(NbtWriter writer) {
        this.writer = writer;
    }

    @Override
    public void visitString(StringTag element) {
        this.writer.putStringEntry(NbtWriter.getStringBytes(element.getAsString()));
    }

    @Override
    public void visitByte(ByteTag element) {
        this.writer.putByteEntry(element.getAsByte());
    }

    @Override
    public void visitShort(ShortTag element) {
        this.writer.putShortEntry(element.getAsShort());
    }

    @Override
    public void visitInt(IntTag element) {
        this.writer.putIntEntry(element.getAsInt());
    }

    @Override
    public void visitLong(LongTag element) {
        this.writer.putLongEntry(element.getAsLong());
    }

    @Override
    public void visitFloat(FloatTag element) {
        this.writer.putFloatEntry(element.getAsFloat());
    }

    @Override
    public void visitDouble(DoubleTag element) {
        this.writer.putDoubleEntry(element.getAsDouble());
    }

    @Override
    public void visitByteArray(ByteArrayTag element) {
        this.writer.putByteArrayEntry(element.getAsByteArray());
    }

    @Override
    public void visitIntArray(IntArrayTag element) {
        this.writer.putIntArrayEntry(element.getAsIntArray());
    }

    @Override
    public void visitLongArray(LongArrayTag element) {
        this.writer.putLongArrayEntry(element.getAsLongArray());
    }

    @Override
    public void visitList(ListTag element) {
        this.writer.startFixedListEntry(element.size(), element.getElementType());
        for (Tag elementBase : element) {
            elementBase.accept(this);
        }
    }

    @Override
    public void visitCompound(CompoundTag compound) {
        visitEntries(compound);
        this.writer.finishCompound();
    }

    public void visitString(byte[] name, StringTag element) {
        this.writer.putString(name, element.getAsString());
    }

    public void visitByte(byte[] name, ByteTag element) {
        this.writer.putByte(name, element.getAsByte());
    }

    public void visitShort(byte[] name, ShortTag element) {
        this.writer.putShort(name, element.getAsShort());
    }

    public void visitInt(byte[] name, IntTag element) {
        this.writer.putInt(name, element.getAsInt());
    }

    public void visitLong(byte[] name, LongTag element) {
        this.writer.putLong(name, element.getAsLong());
    }

    public void visitFloat(byte[] name, FloatTag element) {
        this.writer.putFloat(name, element.getAsFloat());
    }

    public void visitDouble(byte[] name, DoubleTag element) {
        this.writer.putDouble(name, element.getAsDouble());
    }

    public void visitByteArray(byte[] name, ByteArrayTag element) {
        this.writer.putByteArray(name, element.getAsByteArray());
    }

    public void visitIntArray(byte[] name, IntArrayTag element) {
        this.writer.putIntArray(name, element.getAsIntArray());
    }

    public void visitLongArray(byte[] name, LongArrayTag element) {
        this.writer.putLongArray(name, element.getAsLongArray());
    }

    public void visitList(byte[] name, ListTag element) {
        this.writer.startFixedList(name, element.size(), element.getElementType());
        for (Tag elementBase : element) {
            elementBase.accept(this);
        }
    }

    public void visitCompound(byte[] name, CompoundTag compound) {
        this.writer.startCompound(name);
        visitEntries(compound);
        this.writer.finishCompound();
    }

    private void visitEntries(CompoundTag compound) {
        for (Map.Entry<String, Tag> entry : ((INbtCompound) (Object) compound).invokeToMap().entrySet()) {
            this.visit(entry.getKey(), entry.getValue());
        }
    }

    public void visit(String nameBase, Tag element) {
        this.visit(NbtWriter.getStringBytes(nameBase), element);
    }

    public void visit(byte[] nameBase, Tag element) {
        switch (element.getId()) {
            case Tag.TAG_STRING -> this.visitString(nameBase, (StringTag) element);
            case Tag.TAG_BYTE -> this.visitByte(nameBase, (ByteTag) element);
            case Tag.TAG_SHORT -> this.visitShort(nameBase, (ShortTag) element);
            case Tag.TAG_INT -> this.visitInt(nameBase, (IntTag) element);
            case Tag.TAG_LONG -> this.visitLong(nameBase, (LongTag) element);
            case Tag.TAG_FLOAT -> this.visitFloat(nameBase, (FloatTag) element);
            case Tag.TAG_DOUBLE -> this.visitDouble(nameBase, (DoubleTag) element);
            case Tag.TAG_BYTE_ARRAY -> this.visitByteArray(nameBase, (ByteArrayTag) element);
            case Tag.TAG_INT_ARRAY -> this.visitIntArray(nameBase, (IntArrayTag) element);
            case Tag.TAG_LONG_ARRAY -> this.visitLongArray(nameBase, (LongArrayTag) element);
            case Tag.TAG_LIST -> this.visitList(nameBase, (ListTag) element);
            case Tag.TAG_COMPOUND -> this.visitCompound(nameBase, (CompoundTag) element);
            default -> throw new IllegalArgumentException("Unknown NbtElement type: " + element.getId());
        }
    }

    @Override
    public void visitEnd(EndTag element) {
        throw new IllegalArgumentException("Cannot visit null element");
    }
}
