/****
 *
 * VertexBuffer maintains a vertex data array with locations, normals,
 * colors and texcoords.
 *
 ****/
#include <string>
#include <sstream>
#include <cstring>
#include "index_buffer.h"
#include "../util/sxr_log.h"

namespace sxr {

    IndexBuffer::IndexBuffer(int bytesPerIndex, int count)
    : mIndexCount(0),
      mIndexData(NULL),
      mIndexByteSize(0),
      mIsDirty(false),
      mUpdateLock()
    {
        if (bytesPerIndex > 0)
        {
            setIndexSize(bytesPerIndex);
        }
        if (count > 0)
        {
            setIndexCount(count);
        }
    }

    bool IndexBuffer::setIndexSize(int v)
    {
        if ((v != sizeof(short) &&
             v != sizeof(int)))
        {
            LOGE("IndexBuffer: Bytes per index must be 2 or 4, not %d", v);
            return false;
        }
        mIndexByteSize = v;
        return true;
    }

    IndexBuffer::~IndexBuffer()
    {
        if (mIndexData != nullptr)
        {
            free(mIndexData);
            mIndexData = nullptr;
        }
        mIndexCount = 0;
    }


    int IndexBuffer::setShortVec(const unsigned short* src, int srcSize)
    {
        unsigned short*  dest;
        std::lock_guard<std::mutex> lock(mUpdateLock);

        if (src == nullptr)
        {
            LOGE("IndexBuffer: source array not found");
            return 0;
        }
        else
        {
            int rc = setIndexCount(srcSize);
            if (rc <= 0)
            {
                return rc;
            }
        }
        if (mIndexByteSize != sizeof(short))
        {
            LOGE("IndexBuffer: cannot change type of index data");
            return 0;
        }
        dest = reinterpret_cast<unsigned short*>(mIndexData);
        memcpy(dest, src, srcSize * sizeof(short));
        mIsDirty = true;
        return 1;
    }

    int IndexBuffer::setIntVec(const unsigned int* src, int srcSize)
    {
        unsigned int*   dest;
        std::lock_guard<std::mutex> lock(mUpdateLock);

        if (src == nullptr)
        {
            LOGE("IndexBuffer: source array not found");
            return 0;
        }
        else
        {
            int rc = setIndexCount(srcSize);
            if (rc <= 0)
            {
                return rc;
            }
        }
        if (mIndexByteSize != sizeof(int))
        {
            LOGE("IndexBuffer: cannot change type of index data");
            return 0;
        }
        dest = reinterpret_cast<unsigned int*>(mIndexData);
        memcpy(dest, src, srcSize * sizeof(int));
        mIsDirty = true;
        return 1;
    }

    bool    IndexBuffer::getShortVec(unsigned short* dest, int destSize) const
    {
        std::lock_guard<std::mutex> lock(mUpdateLock);
        if (dest == nullptr)
        {
            LOGE("IndexBuffer: source array not provided");
            return false;
        }
        if (mIndexData == nullptr)
        {
            LOGE("IndexBuffer: no indices available");
            return false;
        }
        if (destSize != mIndexCount)
        {
            LOGE("IndexBuffer: destination array is %d entries, expected %d", destSize, mIndexCount);
            return false;
        }
        if (mIndexByteSize != sizeof(short))
        {
            LOGE("IndexBuffer: cannot get short indices, index data is long");
            return false;
        }
        memcpy(dest, mIndexData, destSize * sizeof(short));
        return true;
    }

    bool    IndexBuffer::getIntVec(unsigned int* dest, int destSize) const
    {
        std::lock_guard<std::mutex> lock(mUpdateLock);
        if (dest == nullptr)
        {
            LOGE("IndexBuffer: destination array not provided");
            return false;
        }
        if (mIndexData == nullptr)
        {
            LOGE("IndexBuffer: no indices available");
            return false;
        }
        if (destSize != mIndexCount)
        {
            LOGE("IndexBuffer: destination array is %d entries, expected %d", destSize, mIndexCount);
            return false;
        }
        if (mIndexByteSize != sizeof(int))
        {
            LOGE("IndexBuffer: cannot get integer indices, index data is short");
            return false;
        }
        memcpy(dest, mIndexData, destSize * sizeof(int));
        return true;
    }

    int IndexBuffer::getIndexCount() const
    {
        return mIndexCount;
    }

    int IndexBuffer::setIndexCount(int count)
    {
        if (mIndexByteSize <= 0)
        {
            return 0;
        }
        if ((mIndexCount != 0) && (mIndexCount != count))
        {
            LOGE("IndexBuffer: cannot change size of index array from %d to %d", mIndexCount, count);
            return 0;
        }
        if (mIndexCount == count)
        {
            return 1;
        }
        if (count > 0)
        {
            int datasize = mIndexByteSize * count;
            LOGV("IndexBuffer: %p allocating index buffer of %d bytes\n", this, datasize);
            mIndexData = (char*) malloc(datasize);
            if (mIndexData != nullptr)
            {
                mIndexCount = count;
                return 1;
            }
            LOGE("IndexBuffer: out of memory cannot allocate room for %d bytes\n", datasize);
            return -1;
        }
        LOGE("IndexBuffer: ERROR: no index buffer allocated\n");
        return 0;
    }

    void IndexBuffer::dump() const
    {
        std::ostringstream os;
        int n = 0;

        if ((mIndexData == NULL) || (mIndexCount == 0))
        {
            return;
        }
        for (int i = 0; i < mIndexCount; ++i)
        {
            char* p = mIndexData + i * mIndexByteSize;

            if (n == 8)
            {
                LOGV("%s", os.str().c_str());
                os.clear();
                os.str("");
                n = 0;
            }
            if (mIndexByteSize > 2)
            {
                int tmp = *((int*) p);
                os << tmp << " ";
            }
            else
            {
                short tmp = *((short*) p);
                os <<  tmp << " ";
            }
            ++n;
        }
        LOGV("%s", os.str().c_str());
    }

} // end sxrsdk

