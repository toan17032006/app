import Link from 'next/link';

export const metadata = {
  title: 'Điều khoản dịch vụ — EduSmart',
};

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-slate-50 py-12 px-6">
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white shadow-md">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <h1 className="text-2xl font-bold text-slate-800">Điều khoản dịch vụ</h1>
        </div>

        <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-xs space-y-4 text-sm text-slate-600 leading-relaxed">
          <p>Cập nhật lần cuối: tháng 10/2026</p>
          <p>
            Khi sử dụng EduSmart — nền tảng hỗ trợ học tập (thời gian biểu, bài kiểm tra
            trắc nghiệm, bài tập về nhà, bảng xếp hạng), bạn đồng ý với các điều khoản sau:
          </p>
          <ol className="list-decimal pl-5 space-y-3">
            <li><strong>Tài khoản:</strong> Bạn chịu trách nhiệm bảo vệ thông tin đăng nhập của mình. Không chia sẻ tài khoản cho người khác.</li>
            <li><strong>Nội dung:</strong> Không tải lên nội dung vi phạm pháp luật, xúc phạm người khác hoặc gian lận trong bài kiểm tra.</li>
            <li><strong>Dữ liệu:</strong> Điểm số, xu thưởng và tiến độ học được lưu trên hệ thống để phục vụ trải nghiệm học tập. Bạn có thể yêu cầu xóa tài khoản bất kỳ lúc nào.</li>
            <li><strong>Dịch vụ:</strong> EduSmart cung cấp &quot;nguyên trạng&quot; cho mục đích học tập vàdemo. Chúng tôi không bảo đảm dịch vụ không gián đoạn.</li>
            <li><strong>Thay đổi:</strong> Điều khoản có thể được cập nhật; thay đổi sẽ được công bố trên trang này.</li>
          </ol>
        </div>

        <Link href="/login" className="inline-flex items-center gap-2 text-sm font-semibold text-indigo-600 hover:text-indigo-700">
          <i className="fa-solid fa-arrow-left"></i> Về trang đăng nhập
        </Link>
      </div>
    </div>
  );
}
