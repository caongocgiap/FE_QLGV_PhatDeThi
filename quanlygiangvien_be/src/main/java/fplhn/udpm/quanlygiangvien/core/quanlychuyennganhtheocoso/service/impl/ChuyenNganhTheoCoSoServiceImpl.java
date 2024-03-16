package fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.service.impl;

import fplhn.udpm.quanlygiangvien.core.common.ResponseModel;
import fplhn.udpm.quanlygiangvien.core.quanlybomon.model.response.BoMonResponse;
import fplhn.udpm.quanlygiangvien.core.quanlybomon.repository.DataBoMonRepository;
import fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.model.request.GetChuyenNganhRequest;
import fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.repository.DataChuyenNganhRepository;
import fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.model.request.PostChuyenNganhRequest;
import fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.model.response.ChuyenNganhResponse;
import fplhn.udpm.quanlygiangvien.core.quanlychuyennganhtheocoso.service.ChuyenNganhService;
import fplhn.udpm.quanlygiangvien.entity.BoMon;
import fplhn.udpm.quanlygiangvien.entity.ChuyenNganh;
import fplhn.udpm.quanlygiangvien.infrastructure.constant.XoaMem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class ChuyenNganhServiceImpl implements ChuyenNganhService {

    @Autowired
    private DataChuyenNganhRepository dataChuyenNganhRepository;

    @Autowired
    private DataBoMonRepository dataBoMonRepository;

    @Override
    public ChuyenNganhResponse getChuyenNganh(Long id) {
        Optional<ChuyenNganhResponse> currentChuyenNganh = dataChuyenNganhRepository.getChuyenNganhById(id);
        return currentChuyenNganh.orElse(null);
    }

    @Override
    public Page<ChuyenNganhResponse> getAllList(Long idBoMon, GetChuyenNganhRequest dataRequest) {
        int page = Math.max(dataRequest.getPage(), 1);
        int limit = dataRequest.getLimit();

        Pageable pageable = PageRequest.of(page - 1, limit);

        long startItem = (long) pageable.getPageNumber() * pageable.getPageSize();

        String searchName = null;
        if (dataRequest.getSearchName() != null) {
            searchName = dataRequest.getSearchName().stream()
                    .map(name -> name.replaceAll("[\\\\.*+?\\[\\](){|^$]", "\\\\$0"))
                    .collect(Collectors.joining("|"));
        }

        Page<ChuyenNganhResponse> pages = dataChuyenNganhRepository.getAllChuyenNganh(idBoMon, startItem, pageable, searchName);

        if (pages.getContent().isEmpty() && pages.getTotalPages() < page) {
            page = Math.max(pages.getTotalPages(), 1);
            startItem = (long) pages.getPageable().getPageNumber() * pages.getPageable().getPageSize();
            pages = dataChuyenNganhRepository.getAllChuyenNganh(idBoMon, startItem, PageRequest.of(page - 1, limit), searchName);
        }

        return new PageImpl<>(
                new ArrayList<>(pages.getContent()),
                pages.getPageable(),
                pages.getTotalElements()
        );
    }

    @Override
    public ResponseModel addChuyenNganh(PostChuyenNganhRequest dataRequest) {

        String ten = dataRequest.getTen().trim().replaceAll("\\s+", " ");

        if (ten.isEmpty()) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Tên chuyên ngành không được bỏ trống");
        }

        Optional<BoMonResponse> boMonResponse = dataBoMonRepository.getBoMonById(dataRequest.getIdBoMon());

        if (boMonResponse.isEmpty()) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Bộ môn không tồn tại hoặc đã bị xoá");
        }

        if (dataChuyenNganhRepository.existsByTen(dataRequest.getIdBoMon(), ten)) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Tên chuyên ngành đã tồn tại trên hệ thống");
        }

        BoMon boMon = new BoMon();
        boMon.setId(dataRequest.getIdBoMon());

        ChuyenNganh chuyenNganh = new ChuyenNganh();
        chuyenNganh.setTen(ten);
        chuyenNganh.setBoMon(boMon);
        chuyenNganh.setXoaMem(boMonResponse.get().getTrangThai());

        try {
            dataChuyenNganhRepository.save(chuyenNganh);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Có lỗi xảy ra. Vui lòng thử lại");
        }

        return new ResponseModel(HttpStatus.OK, "Thêm mới thành công chuyên ngành: " + ten);
    }

    @Override
    public ResponseModel updateChuyenNganh(Long id, PostChuyenNganhRequest dataRequest) {
        Optional<ChuyenNganhResponse> currentChuyenNganh = dataChuyenNganhRepository.getChuyenNganhById(id);
        String ten = dataRequest.getTen().trim().replaceAll("\\s+", " ");

        if (currentChuyenNganh.isEmpty()) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Chuyên ngành không tồn tại hoặc đã bị xoá");
        }

        if (ten.isEmpty()) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Tên chuyên ngành không được bỏ trống");
        }

        if (dataChuyenNganhRepository.existsByTenWidthOutId(id, ten)) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Tên chuyên ngành đã tồn tại trên hệ thống");
        }

        BoMon boMon = new BoMon();
        boMon.setId(currentChuyenNganh.get().getIdBoMon());

        ChuyenNganh chuyenNganh = new ChuyenNganh();
        chuyenNganh.setId(id);
        chuyenNganh.setTen(ten);
        chuyenNganh.setBoMon(boMon);
        chuyenNganh.setXoaMem(currentChuyenNganh.get().getTrangThai());

        try {
            dataChuyenNganhRepository.save(chuyenNganh);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Có lỗi xảy ra. Vui lòng thử lại");
        }
        return new ResponseModel(HttpStatus.OK, "Cập nhật chuyên ngành thành công");
    }

    @Override
    public ResponseModel deleteChuyenNganh(Long id) {

        Optional<ChuyenNganhResponse> currentChuyenNganh = dataChuyenNganhRepository.getChuyenNganhById(id);

        if (currentChuyenNganh.isEmpty()) {
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Chuyên ngành không tồn tại hoặc đã bị xoá");
        }

        BoMon boMon = new BoMon();
        boMon.setId(currentChuyenNganh.get().getIdBoMon());

        ChuyenNganh chuyenNganh = new ChuyenNganh();
        chuyenNganh.setId(currentChuyenNganh.get().getId());
        chuyenNganh.setTen(currentChuyenNganh.get().getTen());
        chuyenNganh.setBoMon(boMon);
        chuyenNganh.setXoaMem(XoaMem.DA_XOA);

        if (currentChuyenNganh.get().getTrangThai().equals(XoaMem.CHUA_XOA)) {
            try {
                dataChuyenNganhRepository.save(chuyenNganh);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseModel(HttpStatus.BAD_GATEWAY, "Có lỗi xảy ra. Vui lòng thử lại");
            }
            return new ResponseModel(HttpStatus.OK, "Cập nhật thành công chuyên ngành: " + currentChuyenNganh.get().getTen());
        }

        try {
            dataChuyenNganhRepository.delete(chuyenNganh);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel(HttpStatus.BAD_GATEWAY, "Có lỗi xảy ra. Vui lòng thử lại");
        }

        return new ResponseModel(HttpStatus.OK, "Xoá thành công chuyên ngành: " + currentChuyenNganh.get().getTen());
    }

}
