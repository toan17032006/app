'use client';
import { useState } from 'react';
import ScheduleModal from '@/components/modals/ScheduleModal';
import { DAY_ENUMS, dayLabel, todayEnum } from '@/lib/constants';

export default function Schedule({ scheduleData, onAdd, onEdit, onDelete, onShowAlert }) {
  const [activeDay, setActiveDay] = useState('all');
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);

  const today = todayEnum();

  const filtered =
    activeDay === 'all'
      ? [...scheduleData].sort((a, b) =>
          a.dayOfWeek !== b.dayOfWeek
            ? DAY_ENUMS.indexOf(a.dayOfWeek) - DAY_ENUMS.indexOf(b.dayOfWeek)
            : (a.startTime || '').localeCompare(b.startTime || '')
        )
      : scheduleData.filter((item) => item.dayOfWeek === activeDay);

  const handleOpenAdd = () => {
    setEditItem(null);
    setModalOpen(true);
  };

  const handleOpenEdit = (item) => {
    setEditItem(item);
    setModalOpen(true);
  };

  // onAdd/onEdit trả về { success } — chỉ báo thành công sau khi API thật sự xong
  const handleSave = async (form, editId) => {
    const result = editId ? await onEdit(editId, form) : await onAdd(form);
    if (result?.success) {
      onShowAlert(
        editId ? 'Cập nhật thành công' : 'Thêm thành công',
        editId
          ? 'Thời gian biểu đã được lưu lại.'
          : 'Tiết học mới đã được thêm vào thời gian biểu.'
      );
      setModalOpen(false);
    } else {
      onShowAlert('Không thể lưu', result?.error || 'Đã có lỗi xảy ra khi lưu tiết học.');
    }
  };

  const handleDelete = async (id) => {
    const result = await onDelete(id);
    if (result?.success) {
      onShowAlert('Đã xóa', 'Tiết học đã được loại bỏ khỏi thời gian biểu.');
    } else {
      onShowAlert('Không thể xóa', result?.error || 'Đã có lỗi xảy ra khi xóa tiết học.');
    }
  };

  return (
    <section className="space-y-6 fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <h2 className="text-xl font-bold text-slate-800">
            <i className="fa-solid fa-calendar-days text-indigo-600 mr-2"></i> Thời gian biểu cá nhân
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý lịch học chi tiết trong tuần từ Thứ Hai đến Chủ Nhật.
          </p>
        </div>
        <button
          onClick={handleOpenAdd}
          className="px-5 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl transition shadow-md shadow-indigo-600/20 flex items-center gap-2"
        >
          <i className="fa-solid fa-plus"></i> Thêm tiết học / Sự kiện
        </button>
      </div>

      {/* Day Filter Tabs */}
      <div className="flex overflow-x-auto gap-2 pb-2">
        <button
          onClick={() => setActiveDay('all')}
          className={`px-4 py-2.5 rounded-xl font-semibold text-sm whitespace-nowrap transition ${
            activeDay === 'all'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
          }`}
        >
          Tất cả các ngày
        </button>
        {DAY_ENUMS.map((d) => (
          <button
            key={d}
            onClick={() => setActiveDay(d)}
            className={`px-4 py-2.5 rounded-xl font-semibold text-sm whitespace-nowrap transition ${
              activeDay === d
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {dayLabel(d)}
            {d === today && <span className="ml-1 text-[10px] text-amber-500">•</span>}
          </button>
        ))}
      </div>

      {/* Schedule Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filtered.length === 0 ? (
          <div className="col-span-full py-12 text-center text-slate-400 text-sm">
            Chưa có tiết học nào được thêm vào thời gian biểu này.
          </div>
        ) : (
          filtered.map((item) => (
            <div
              key={item.id}
              className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between space-y-4 hover:shadow-md transition"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase px-2.5 py-1 bg-indigo-50 text-indigo-600 rounded-md">
                    {dayLabel(item.dayOfWeek)}
                  </span>
                  <span className="text-xs font-semibold px-2.5 py-1 bg-slate-100 text-slate-700 rounded-md">
                    <i className="fa-regular fa-clock mr-1 text-slate-400"></i>
                    {item.startTime} - {item.endTime}
                  </span>
                </div>
                <h3 className="font-bold text-base text-slate-800">{item.subject}</h3>
                <p className="text-xs text-slate-600">
                  <i className="fa-solid fa-chalkboard-user mr-1.5 text-slate-400"></i>
                  {item.teacher}
                </p>
                {item.note && (
                  <p className="text-xs italic text-slate-400 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                    📝 {item.note}
                  </p>
                )}
              </div>
              <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
                <button
                  onClick={() => handleOpenEdit(item)}
                  className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition"
                >
                  <i className="fa-solid fa-pen mr-1"></i> Sửa
                </button>
                <button
                  onClick={() => handleDelete(item.id)}
                  className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 text-xs font-semibold rounded-lg transition"
                >
                  <i className="fa-solid fa-trash mr-1"></i> Xóa
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Schedule Modal */}
      <ScheduleModal
        isOpen={modalOpen}
        editItem={editItem}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
      />
    </section>
  );
}
