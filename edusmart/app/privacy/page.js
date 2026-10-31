import Link from 'next/link';

export const metadata = {
  title: 'Chính sách bảo mật — EduSmart',
};

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-slate-50 py-12 px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white shadow-md">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <h1 className="text-2xl font-bold text-slate-800">Chính sách bảo mật</h1>
        </div>

        <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-xs space-y-4 text-sm text-slate-600 leading-relaxed">
          <p>Cập nhật lần cuối: tháng 10/2026</p>
          <ol className="list-decimal pl-5 space-y-3">
            <li><strong>Thông tin thu thập:</strong> Họ tên, email, lớp học, ảnh đại diện (nếu đăng nhập bằng Google) cùng dữ liệu học tập như điểm số, xu thưởng, thời gian biểu và bài tập bạn nộp.</li>
            <li><strong>Mục đích sử dụng:</strong> Cung cấp tính năng học tập (chấm điểm, xếp hạng, nhắc deadline), hỗ trợ và cải thiện dịch vụ.</li>
            <li><strong>Mật khẩu:</strong> Được mã hóa bằng thuật toán BCrypt — kể cả quản trị viên cũng không thể đọc được mật khẩu gốc của bạn.</li>
            <li><strong>Chia sẻ:</strong> Không bán hay chia sẻ dữ liệu cá nhân cho bên thứ ba. Họ tên và điểm tích lũy chỉ hiển thị trong bảng xếp hạng của chính trường/hệ thống.</li>
            <li><strong>Lưu trữ:</strong> Dữ liệu lưu trong cơ sở dữ liệu của hệ thống. File bài tập bạn nộp chỉ hiển thị với bạn (qua link riêng).</li>
            <li><strong>Quyền của bạn:</strong> Bạn có thể xem, sửa thông tin cá nhân hoặc yêu cầu xóa tài khoản bất kỳ lúc nào.</li>
          </ol>
        </div>

        <Link href="/login" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700">
          <i className="fa-solid fa-arrow-left"></i> Về trang đăng nhập
        </Link>
      </div>
    </div>
  );
}
