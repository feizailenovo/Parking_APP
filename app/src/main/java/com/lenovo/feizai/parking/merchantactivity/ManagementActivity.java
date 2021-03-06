package com.lenovo.feizai.parking.merchantactivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.lenovo.feizai.parking.R;
import com.lenovo.feizai.parking.base.BaseActivity;
import com.lenovo.feizai.parking.base.BaseModel;
import com.lenovo.feizai.parking.base.BaseObserver;
import com.lenovo.feizai.parking.base.BaseRecyclerView;
import com.lenovo.feizai.parking.base.BaseRefreshRecyclerView;
import com.lenovo.feizai.parking.camera.CaptureActivity;
import com.lenovo.feizai.parking.customeractivity.CustomerMainActivity;
import com.lenovo.feizai.parking.dialog.OptionDialog;
import com.lenovo.feizai.parking.dialog.ShowPhotoDialog;
import com.lenovo.feizai.parking.entity.CheckInfo;
import com.lenovo.feizai.parking.entity.Order;
import com.lenovo.feizai.parking.entity.ParkingSpace;
import com.lenovo.feizai.parking.net.ExceptionHandle;
import com.lenovo.feizai.parking.net.RetrofitClient;
import com.lenovo.feizai.parking.util.DensityUtil;
import com.lenovo.feizai.parking.util.EncodingUtils;
import com.lenovo.feizai.parking.util.GlideEngine;
import com.lenovo.feizai.parking.util.GsonUtil;
import com.lenovo.feizai.parking.util.ToolUtil;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.orhanobut.logger.Logger;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;

import static com.lenovo.feizai.parking.camera.CaptureActivity.REQUEST_QR_CODE;
import static com.lenovo.feizai.parking.merchantactivity.CheckCarLicenseActivity.REQUEST_IN_CAR_LICENSE;
import static com.lenovo.feizai.parking.merchantactivity.CheckCarLicenseActivity.REQUEST_OUT_CAR_LICENSE;

/**
 * @author feizai
 * @date 2021/4/1 0001 ?????? 9:14:28
 * @annotation
 */
public class ManagementActivity extends BaseActivity {

    @BindView(R.id.title)
    TextView title;

    private RetrofitClient client;
    private String merchant;
    private BaseRefreshRecyclerView space_list;
    private String[] permissions = {
            Manifest.permission.CAMERA,
    };

    public ManagementActivity() {
        super(R.layout.activity_management);
    }

    @Override
    protected void initView() {
        Intent intent = getIntent();
        merchant = intent.getStringExtra("name");
        title.setText(merchant);
        client = RetrofitClient.getInstance(this);

        space_list = new BaseRefreshRecyclerView(this, R.id.list_item, R.id.spacerefresh) {
            @Override
            public BaseQuickAdapter initAdapter() {
                class ParkingSpaceAdapter extends BaseQuickAdapter<ParkingSpace, BaseViewHolder> {

                    public ParkingSpaceAdapter(@org.jetbrains.annotations.Nullable List<ParkingSpace> data) {
                        super(R.layout.table_item, data);
                    }

                    @Override
                    protected void convert(@NotNull BaseViewHolder baseViewHolder, ParkingSpace parkingSpace) {
                        TextView text = baseViewHolder.getView(R.id.serial);
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) text.getLayoutParams();
                        params.width = (DensityUtil.getSreenWidth(ManagementActivity.this) - 65) / 10;
                        params.height = (DensityUtil.getSreenWidth(ManagementActivity.this) - 65) / 10;
                        text.setLayoutParams(params);
                        baseViewHolder.setText(R.id.serial, parkingSpace.getSerialnumber() + "");
                        String parkingstate = parkingSpace.getParkingstate();
                        switch (parkingstate) {
                            case "?????????":
                                baseViewHolder.setBackgroundResource(R.id.bg, R.drawable.subscribe);
                                break;
                            case "?????????":
                                baseViewHolder.setBackgroundResource(R.id.bg, R.drawable.subscribed);
                                break;
                            case "?????????":
                                baseViewHolder.setBackgroundResource(R.id.bg, R.drawable.used);
                                break;
                            case "?????????":
                                baseViewHolder.setBackgroundResource(R.id.bg, R.drawable.unused);
                                break;
                        }
                    }
                }
                ParkingSpaceAdapter adapter = new ParkingSpaceAdapter(null);
                return adapter;
            }
        };

        space_list.setItemDecoration(new GridSpacingItemDecoration(10, 5, false));
        space_list.setLayoutManager(new GridLayoutManager(this, 10));

        space_list.setOnItemClick(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                ParkingSpace parkingSpace = (ParkingSpace) adapter.getItem(position);
                MaterialDialog dialog = new MaterialDialog(ManagementActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR());
                View bg = view.findViewById(R.id.bg);
                Timer timer = new Timer();
                switch (parkingSpace.getParkingstate()) {
                    case "?????????":
                        showToast("????????????????????????");
                        break;
                    case "?????????":
                        client.findOrderByNumber(parkingSpace.getRemark(), new BaseObserver<BaseModel<String>>(ManagementActivity.this) {
                            @Override
                            protected void showDialog() {

                            }

                            @Override
                            protected void hideDialog() {

                            }

                            @Override
                            protected void successful(BaseModel<String> stringBaseModel) {
                                View dialogView = LayoutInflater.from(ManagementActivity.this).inflate(R.layout.dialog_subscribe_detailed_info, null, false);
                                TextView orderState = dialogView.findViewById(R.id.orderState);
                                TextView orderNumber = dialogView.findViewById(R.id.orderNumber);
                                TextView space = dialogView.findViewById(R.id.space);
                                TextView duration = dialogView.findViewById(R.id.duration);
                                TextView startdate = dialogView.findViewById(R.id.startdate);
                                TextView enddate = dialogView.findViewById(R.id.enddate);
                                Order order = GsonUtil.GsonToBean(stringBaseModel.getData(), Order.class);
                                orderState.setText(order.getState());
                                orderNumber.setText(order.getOrderNumber());
                                space.setText(order.getSpace());
                                duration.setText(order.getDuration() + "??????");
                                startdate.setText(ToolUtil.timeStampToString(order.getStartDate()));
                                enddate.setText(ToolUtil.timeStampToString(order.getEndDate()));
                                dialog.addContentView(dialogView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                dialog.show();
                            }

                            @Override
                            protected void defeated(BaseModel<String> stringBaseModel) {
                                dialog.title(null, "??????");
                                dialog.icon(R.drawable.ic_error, null);
                                dialog.message(null, "??????????????????", null);
                            }

                            @Override
                            public void onError(ExceptionHandle.ResponeThrowable e) {
                                showToast(e.getMessage());
                                Logger.e(e, e.toString());
                            }
                        });
                        break;
                    case "?????????":
                        client.selectUsingSpace(parkingSpace, new BaseObserver<BaseModel<CheckInfo>>(ManagementActivity.this) {
                            @Override
                            protected void showDialog() {

                            }

                            @Override
                            protected void hideDialog() {

                            }

                            @Override
                            protected void successful(BaseModel<CheckInfo> checkInfoBaseModel) {
                                View dialogView = LayoutInflater.from(ManagementActivity.this).inflate(R.layout.dialog_using_detailed_info, null, false);
                                TextView orderState = dialogView.findViewById(R.id.orderState);
                                TextView car = dialogView.findViewById(R.id.car_license);
                                TextView space = dialogView.findViewById(R.id.space);
                                TextView duration = dialogView.findViewById(R.id.duration);
                                TextView startdate = dialogView.findViewById(R.id.startdate);
                                CheckInfo checkInfo = checkInfoBaseModel.getData();
                                orderState.setText("???????????????");
                                car.setText(checkInfo.getCarlicense());
                                space.setText(checkInfo.getSerialnumber());
                                startdate.setText(checkInfo.getIntime());
                                if (checkInfo.getState().equals("?????????")) {
                                    TimerTask task = new TimerTask() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    duration.setText(ToolUtil.positiveTime(checkInfo.getIntime()));
                                                }
                                            });
                                        }
                                    };
                                    timer.schedule(task, new Date(), 1000);
                                } else {
                                    orderState.setText("????????????????????????");
                                    duration.setText(ToolUtil.getDetailDuration(checkInfo.getIntime(),checkInfo.getOuttime()));
                                }
                                dialog.addContentView(dialogView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                dialog.show();
                                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        timer.cancel();
                                    }
                                });
                            }

                            @Override
                            protected void defeated(BaseModel<CheckInfo> checkInfoBaseModel) {
                                dialog.title(null, "??????");
                                dialog.icon(R.drawable.ic_error, null);
                                dialog.message(null, "??????????????????", null);
                            }

                            @Override
                            public void onError(ExceptionHandle.ResponeThrowable e) {
                                showToast(e.getMessage());
                                Logger.e(e, e.toString());
                            }
                        });
                        break;
                    case "?????????":
                        showToast("?????????????????????");
                        break;
                }
            }
        });

        space_list.addRefreshLisenter(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                space_list.cleanData();
                initData();
            }
        });
    }

    @OnClick(R.id.title)
    public void title() {
        Bitmap qrCode = EncodingUtils.createQRCode(title.getText().toString().trim(), 100, 100, null);
        ShowPhotoDialog dialog = new ShowPhotoDialog(this);
        dialog.setPhoto(qrCode);
        dialog.show();
    }

    @OnClick(R.id.setting)
    public void set() {
        Intent intent = new Intent(ManagementActivity.this, ParkingSettingActivity.class);
        intent.putExtra("name", merchant);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent();
        switch (requestCode) {
            case REQUEST_IN_CAR_LICENSE:
                if (resultCode == RESULT_OK) {
                    String car = data.getStringExtra("car");
                    intent.setClass(ManagementActivity.this, CheckInActivity.class);
                    intent.putExtra("order", "");
                    intent.putExtra("merchant", merchant);
                    intent.putExtra("car", car);
                    startActivity(intent);
                }
                break;
            case REQUEST_OUT_CAR_LICENSE:
                if (resultCode == RESULT_OK) {
                    String car = data.getStringExtra("car");
                    client.selectCheckInfoByCar(merchant, car, new BaseObserver<BaseModel<CheckInfo>>(this) {
                        @Override
                        protected void showDialog() {

                        }

                        @Override
                        protected void hideDialog() {

                        }

                        @Override
                        protected void successful(BaseModel<CheckInfo> checkInfoBaseModel) {
                            switch (checkInfoBaseModel.getMessage()) {
                                case "?????????":
                                    showToast("?????????????????????????????????");
                                    initData();
                                    break;
                                case "?????????":
                                    CheckInfo info = checkInfoBaseModel.getData();
                                    intent.setClass(ManagementActivity.this, CheckOutActivity.class);
                                    intent.putExtra("car", car);
                                    intent.putExtra("merchant", merchant);
                                    intent.putExtra("info", GsonUtil.GsonString(info));
                                    startActivity(intent);
                                    break;
                            }

                        }

                        @Override
                        protected void defeated(BaseModel<CheckInfo> checkInfoBaseModel) {
                            showToast("?????????????????????");
                        }

                        @Override
                        public void onError(ExceptionHandle.ResponeThrowable e) {
                            Logger.e(e, e.getMessage());
                        }
                    });
                }
                break;
            case REQUEST_QR_CODE:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        String result = data.getStringExtra("result");
                        try {
                            Order order = GsonUtil.GsonToBean(result, Order.class);
                            if (order.getMerchantName().equals(merchant)) {
                                client.addCheckInfoByQRCode(order.getOrderNumber(), new BaseObserver<BaseModel>(ManagementActivity.this) {
                                    @Override
                                    protected void showDialog() {

                                    }

                                    @Override
                                    protected void hideDialog() {

                                    }

                                    @Override
                                    protected void successful(BaseModel baseModel) {
                                        showToast(baseModel.getMessage());
                                        initData();
                                    }

                                    @Override
                                    protected void defeated(BaseModel baseModel) {
                                        switch (baseModel.getMessage()) {
                                            case "???????????????"://?????????
                                                showToast("??????????????????");
                                                intent.setClass(ManagementActivity.this, CheckInActivity.class);
                                                intent.putExtra("order", GsonUtil.GsonString(order));
                                                intent.putExtra("merchant", merchant);
                                                intent.putExtra("car", order.getCarLicense());
                                                startActivity(intent);
                                                break;
                                            case "???????????????":
                                                showToast("??????????????????");
//                                            intent.setClass(ManagementActivity.this, CheckInActivity.class);
//                                            intent.putExtra("order", GsonUtil.GsonString(order));
//                                            intent.putExtra("merchant", merchant);
//                                            intent.putExtra("car", order.getCarLicense());
//                                            startActivity(intent);
                                                break;
                                            case "???????????????":
                                                showToast("??????????????????");
                                                break;
                                        }
                                    }

                                    @Override
                                    public void onError(ExceptionHandle.ResponeThrowable e) {
                                        Log.e("tag", e.getMessage());
                                    }
                                });
                            } else {
                                MaterialDialog dialog = new MaterialDialog(ManagementActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR());
                                dialog.title(null, "??????");
                                dialog.icon(null, getResources().getDrawable(R.drawable.ic_error));
                                dialog.message(null, "?????????????????????", null);
                                dialog.show();
                            }
                        } catch (Exception e) {
                            MaterialDialog dialog = new MaterialDialog(ManagementActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR());
                            dialog.title(null, "??????");
                            dialog.icon(null, getResources().getDrawable(R.drawable.ic_error));
                            dialog.message(null, "?????????????????????", null);
                            dialog.show();
                        }
                    }
                }
                break;
        }
    }

    @OnClick(R.id.back)
    public void back() {
        finish();
    }

    @OnClick(R.id.into)
    public void into() {
        OptionDialog dialog = new OptionDialog(this);
        dialog.setShowTitle(false);
        dialog.setFirstOptionTxt("?????????");
        dialog.setSecondOptionTxt("????????????");
        dialog.setOnBtnClickLister(new OptionDialog.OnBtnClickLister() {
            @Override
            public void first() {
                startActivityForResult(CheckCarLicenseActivity.class, REQUEST_IN_CAR_LICENSE);
            }

            @Override
            public void second() {
                requestPermission(2, permissions, new Runnable() {
                    @Override
                    public void run() {
                        startActivityForResult(CaptureActivity.class, REQUEST_QR_CODE);
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        showToast("????????????????????????????????????");
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        MaterialDialog dialog = new MaterialDialog(ManagementActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR());
                        dialog.title(null, "??????");
                        dialog.icon(null, getResources().getDrawable(R.drawable.ic_error));
                        dialog.message(null, "????????????????????????????????????", dialogMessageSettings -> {
                            return null;
                        });
                        dialog.positiveButton(null, "??????", materialDialog -> {
                            startActivity(getAppDetailSettingIntent());
                            return null;
                        });
                        dialog.negativeButton(null, "??????", materialDialog -> {

                            return null;
                        });
                        dialog.show();
                    }
                });
            }

            @Override
            public void cancle() {

            }
        });
        dialog.show();
    }

    @OnClick(R.id.outto)
    public void outto() {
        startActivityForResult(CheckCarLicenseActivity.class, REQUEST_OUT_CAR_LICENSE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initData();
    }

    private void initData() {
        client.searchParkingSpace(merchant, new BaseObserver<BaseModel<ParkingSpace>>(this) {
            @Override
            protected void showDialog() {

            }

            @Override
            protected void hideDialog() {

            }

            @Override
            protected void successful(BaseModel<ParkingSpace> parkingSpaceBaseModel) {
                List<ParkingSpace> datas = parkingSpaceBaseModel.getDatas();
                space_list.replaceData(datas);
                space_list.refreshEnd();
            }

            @Override
            protected void defeated(BaseModel<ParkingSpace> parkingSpaceBaseModel) {
                showToast(parkingSpaceBaseModel.getMessage());
                space_list.refreshEnd();
            }

            @Override
            public void onError(ExceptionHandle.ResponeThrowable e) {
                Log.e("tag", e.getMessage());
                space_list.refreshEnd();
            }
        });
    }

    //????????????????????????intent??????????????????????????????????????????????????????????????????????????????????????????
    private Intent getAppDetailSettingIntent() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        return localIntent;
    }
}
