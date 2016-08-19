/*******************************************************************************
 * Copyright (C) 2016 Sam Silverberg sam.silverberg@gmail.com	
 *
 * This file is part of OpenDedupe SDFS.
 *
 * OpenDedupe SDFS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenDedupe SDFS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.opendedup.collections;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LongKeyValue implements Externalizable {
	private byte[] value;
	private long key;

	public LongKeyValue(long key, byte[] value) {
		this.key = key;
		this.value = value;
	}

	public LongKeyValue() {

	}

	@Override
	public void readExternal(ObjectInput out) throws IOException,
			ClassNotFoundException {
		this.key = out.readLong();
		this.value = new byte[out.readInt()];
		out.readFully(value);
	}

	@Override
	public void writeExternal(ObjectOutput in) throws IOException {
		in.writeLong(this.key);
		in.writeInt(value.length);
		in.write(value);
	}

	public byte[] getValue() {
		return value;
	}

	public long getKey() {
		return key;
	}

}
