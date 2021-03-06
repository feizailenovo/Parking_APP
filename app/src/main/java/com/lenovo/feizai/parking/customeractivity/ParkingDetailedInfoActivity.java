package com.lenovo.feizai.parking.customeractivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baidu.mapapi.model.LatLng;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnLoadMoreListener;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.lenovo.feizai.parking.R;
import com.lenovo.feizai.parking.base.BaseActivity;
import com.lenovo.feizai.parking.base.BaseModel;
import com.lenovo.feizai.parking.base.BaseObserver;
import com.lenovo.feizai.parking.base.BaseRefreshRecyclerView;
import com.lenovo.feizai.parking.dialog.InputTextMsgDialog;
import com.lenovo.feizai.parking.dialog.ShowPhotoDialog;
import com.lenovo.feizai.parking.entity.CollectionInfo;
import com.lenovo.feizai.parking.entity.Comment;
import com.lenovo.feizai.parking.entity.Location;
import com.lenovo.feizai.parking.entity.MerchantState;
import com.lenovo.feizai.parking.entity.ParkingInfo;
import com.lenovo.feizai.parking.entity.MerchantProperty;
import com.lenovo.feizai.parking.entity.ParkingNumber;
import com.lenovo.feizai.parking.net.ExceptionHandle;
import com.lenovo.feizai.parking.net.RequestAPI;
import com.lenovo.feizai.parking.net.RetrofitClient;
import com.lenovo.feizai.parking.util.DensityUtil;
import com.lenovo.feizai.parking.util.GsonUtil;
import com.lenovo.feizai.parking.util.ToolUtil;
import com.youth.banner.Banner;
import com.youth.banner.adapter.BannerImageAdapter;
import com.youth.banner.holder.BannerImageHolder;
import com.youth.banner.indicator.CircleIndicator;
import com.youth.banner.listener.OnBannerListener;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * @author feizai
 * @date 2021/3/16 0016 ?????? 5:44:20
 * @annotation
 */
public class ParkingDetailedInfoActivity extends BaseActivity {

    @BindView(R.id.collection_img)
    ImageView collection_img;
    @BindView(R.id.merchant)
    TextView merchantname;
    @BindView(R.id.state)
    TextView state;

    Banner banner;
    TextView max_number;
    TextView parking_address;
    TextView distance;
    LinearLayout subscribe;

    private Location location;
    private LatLng nowLatlng;
    private int subscribenumber;
    private ParkingInfo parkingInfo;
    private RetrofitClient client;
    private BaseRefreshRecyclerView comment_list;
    private List<Comment> comments;
    private int index;
    private Boolean isOpen;

    public ParkingDetailedInfoActivity() {
        super(R.layout.activity_parking_detailed_info);
    }

    @Override
    protected void initView() {
        Intent intent = getIntent();
        String locationjson = intent.getStringExtra("location");
        String nowLatlngjson = intent.getStringExtra("nowLatlng");
        location = GsonUtil.GsonToBean(locationjson, Location.class);
        client = RetrofitClient.getInstance(ParkingDetailedInfoActivity.this);
        comments = new ArrayList<>();
        index = 0;
        isOpen = false;
        View view = LayoutInflater.from(this).inflate(R.layout.activity_parking_detailed_info_head, null, false);
        initHead(view);

        if (!TextUtils.isEmpty(nowLatlngjson))
            nowLatlng = GsonUtil.GsonToBean(nowLatlngjson, LatLng.class);
        else
            nowLatlng = null;

        comment_list = new BaseRefreshRecyclerView(this, R.id.comments, R.id.comments_refresh) {
            class CommentAdapter extends BaseQuickAdapter<Comment, BaseViewHolder> implements LoadMoreModule {
                private Context context;

                public CommentAdapter(Context context, @Nullable List<Comment> data) {
                    super(R.layout.item_comment_new, data);
                    this.context = context;
                }

                @Override
                protected void convert(@NotNull BaseViewHolder baseViewHolder, Comment comment) {
                    CircleImageView circleImageView = baseViewHolder.getView(R.id.iv_header);
                    baseViewHolder.setText(R.id.tv_user_name, comment.getUsername());
                    baseViewHolder.setText(R.id.tv_content, comment.getContainer());
                    baseViewHolder.setText(R.id.tv_time, ToolUtil.getShowTime(comment.getCommenttime()));
                    Glide.with(context).load(RequestAPI.baseImageURL + comment.getAvatar()).placeholder(R.mipmap.avatar).into(circleImageView);
                }
            }

            @Override
            public BaseQuickAdapter initAdapter() {
                return new CommentAdapter(ParkingDetailedInfoActivity.this, null);
            }
        };

        comment_list.addHeadView(view);
        comment_list.setItemDecoration(new HorizontalDividerItemDecoration.Builder(this).color(Color.GRAY).size(1).build());
        comment_list.enableLoadMore(true);
        comment_list.loadAutoMore(false);
        comment_list.loadMore(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                initComment();
            }
        });
        comment_list.addRefreshLisenter(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                index = 0;
                comment_list.cleanData();
                comments = new ArrayList<>();
                initComment();
            }
        });
    }

    private void initHead(View view) {
        banner = view.findViewById(R.id.top_banner);
        max_number = view.findViewById(R.id.max_number);
        parking_address = view.findViewById(R.id.parking_address);
        distance = view.findViewById(R.id.distance);
        subscribe = view.findViewById(R.id.subscribe);
        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
            }
        });
    }

    @OnClick(R.id.back)
    public void back() {
        finish();
    }

    @OnClick(R.id.navigation)
    public void navigation() {
        if (nowLatlng == null) {
            Toast.makeText(ParkingDetailedInfoActivity.this, "??????????????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        BNRoutePlanNode sNode = new BNRoutePlanNode.Builder()
                .latitude(nowLatlng.latitude)
                .longitude(nowLatlng.longitude)
                .build();
        BNRoutePlanNode eNode = new BNRoutePlanNode.Builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
        List<BNRoutePlanNode> list = new ArrayList<>();
        list.add(sNode);
        list.add(eNode);
        BaiduNaviManagerFactory.getRoutePlanManager().routePlanToNavi(
                list,
                IBNRoutePlanManager.RoutePlanPreference.ROUTE_PLAN_PREFERENCE_DEFAULT,
                null,
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_START:
                                Toast.makeText(ParkingDetailedInfoActivity.this.getApplicationContext(),
                                        "????????????", Toast.LENGTH_SHORT).show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_SUCCESS:
                                Toast.makeText(ParkingDetailedInfoActivity.this.getApplicationContext(),
                                        "????????????", Toast.LENGTH_SHORT).show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_FAILED:
                                Toast.makeText(ParkingDetailedInfoActivity.this.getApplicationContext(),
                                        "????????????", Toast.LENGTH_SHORT).show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_TO_NAVI:
                                Toast.makeText(ParkingDetailedInfoActivity.this.getApplicationContext(),
                                        "??????????????????????????????", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ParkingDetailedInfoActivity.this,
                                        DemoGuideActivity.class);
                                startActivity(intent);
                                break;
                            default:
                                // nothing
                                break;
                        }
                    }
                });
    }

    public void subscribe() {
        if (!isOpen) {
            showToast("???????????????????????????????????????!!!");
            return;
        }
        if (subscribenumber > 0) {
            Intent intent = new Intent(this, SubscribeActivity.class);
            intent.putExtra("merchant", location.getMerchantname());
            startActivity(intent);
        } else {
            MaterialDialog dialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR());
            dialog.title(null, "??????");
            dialog.message(null, "???????????????????????????", dialogMessageSettings -> {
                return null;
            });
            dialog.show();
        }
    }

    @OnClick(R.id.collection)
    public void collection() {
        CollectionInfo info = new CollectionInfo();
        info.setUsername(ToolUtil.getUsername(this));
        info.setAddress(parkingInfo.getMerchantaddress());
        info.setRemark(parkingInfo.getMerchantname());
        info.setLatitude(location.getLatitude());
        info.setLongitude(location.getLongitude());
        if (collection_img.isSelected()) {
            collection_img.setSelected(!collection_img.isSelected());
            setCollection(2, info);
        } else {
            collection_img.setSelected(!collection_img.isSelected());
            setCollection(1, info);
        }
    }

    @OnClick(R.id.comment)
    public void comment() {
        InputTextMsgDialog dialog = new InputTextMsgDialog(this, R.style.dialog_center);
        dialog.setMaxNumber(10);
        dialog.setHint("?????????????????????????????????????????????");
        dialog.setmOnTextSendListener(new InputTextMsgDialog.OnTextSendListener() {
            @Override
            public void onTextSend(String msg) {
                //??????????????????????????????????????????msg???????????????
                Comment comment = new Comment();
                comment.setUsername(ToolUtil.getUsername(ParkingDetailedInfoActivity.this));
                comment.setMerchantname(location.getMerchantname());
                comment.setContainer(msg);
                comment.setCommenttime(ToolUtil.getDate());
                comment.setAvatar(ToolUtil.getUsernameAvatar(ParkingDetailedInfoActivity.this));
                client.addComment(comment, new BaseObserver<BaseModel>(ParkingDetailedInfoActivity.this) {
                    @Override
                    protected void showDialog() {

                    }

                    @Override
                    protected void hideDialog() {

                    }

                    @Override
                    protected void successful(BaseModel baseModel) {
                        showToast("??????????????????");
                        comments.clear();
                        index = 0;
                        initComment();
                    }

                    @Override
                    protected void defeated(BaseModel baseModel) {
                        showToast("??????????????????");
                    }

                    @Override
                    public void onError(ExceptionHandle.ResponeThrowable e) {
                        Log.e("tag", e.getMessage());
                    }
                });
            }
        });
        dialog.show();
    }

    private void setCollection(int code, CollectionInfo info) {
        switch (code) {
            case 1: {
                client.addCollection(info, new BaseObserver<BaseModel>(ParkingDetailedInfoActivity.this) {
                    @Override
                    protected void showDialog() {

                    }

                    @Override
                    protected void hideDialog() {

                    }

                    @Override
                    protected void successful(BaseModel baseModel) {
                        showToast("?????????");
                    }

                    @Override
                    protected void defeated(BaseModel baseModel) {
                        showToast(baseModel.getMessage());
                    }

                    @Override
                    public void onError(ExceptionHandle.ResponeThrowable e) {

                    }
                });
            }
            break;
            case 2: {
                client.deleteCollection(info, new BaseObserver<BaseModel>(ParkingDetailedInfoActivity.this) {
                    @Override
                    protected void showDialog() {

                    }

                    @Override
                    protected void hideDialog() {

                    }

                    @Override
                    protected void successful(BaseModel baseModel) {
                        showToast("????????????");
                    }

                    @Override
                    protected void defeated(BaseModel baseModel) {
                        showToast(baseModel.getMessage());
                    }

                    @Override
                    public void onError(ExceptionHandle.ResponeThrowable e) {

                    }
                });
            }
            break;
        }
    }

    private void initComment(){
        client.selectCommentByMerchant(location.getMerchantname(), index, new BaseObserver<BaseModel<Comment>>(this) {
            @Override
            protected void showDialog() {

            }

            @Override
            protected void hideDialog() {

            }

            @Override
            protected void successful(BaseModel<Comment> commentBaseModel) {
                comment_list.refreshEnd();
                List<Comment> result = commentBaseModel.getDatas();
                if (result.size() > 0) {
                    for (Comment comment : result) {
                        comments.add(comment);
                        comment_list.replaceData(comments);
                    }
                    if (result.size() < 10) {
                        comment_list.loadEnd();
                    } else {
                        comment_list.loadComplete();
                    }
                } else {
                    showToast("?????????????????????");
                    comment_list.loadEnd();
                }
                index += 10;
            }

            @Override
            protected void defeated(BaseModel<Comment> commentBaseModel) {
                comment_list.refreshEnd();
                comment_list.loadEnd();
            }

            @Override
            public void onError(ExceptionHandle.ResponeThrowable e) {
                comment_list.loadFail();
                Log.e("tag",e.getMessage());
            }
        });
    }

    private void initData() {
        client.isCollection(ToolUtil.getUsername(this), location.getMerchantname(), new BaseObserver<BaseModel>(this) {
            @Override
            protected void showDialog() {

            }

            @Override
            protected void hideDialog() {

            }

            @Override
            protected void successful(BaseModel baseModel) {
                collection_img.setSelected(true);
            }

            @Override
            protected void defeated(BaseModel baseModel) {
                collection_img.setSelected(false);
            }

            @Override
            public void onError(ExceptionHandle.ResponeThrowable e) {
                Log.e("tag", e.getMessage());
            }
        });

        client.searchMerchant(location.getMerchantname(), new BaseObserver<BaseModel<MerchantProperty>>(ParkingDetailedInfoActivity.this) {

            @Override
            protected void showDialog() {

            }

            @Override
            protected void hideDialog() {

            }

            @Override
            protected void successful(BaseModel<MerchantProperty> merchantPropertyBaseModel) {
                MerchantProperty merchantProperty = merchantPropertyBaseModel.getDatas().get(0);
                ParkingInfo parkingInfo = merchantProperty.getParkingInfo();
                Location location = merchantProperty.getLocation();
                ParkingNumber parkingNumber = merchantProperty.getParkingNumber();
                ParkingDetailedInfoActivity.this.parkingInfo = parkingInfo;
                MerchantState merchantState = merchantProperty.getMerchantState();
                if (TextUtils.equals(merchantState.getOperatingstate(), "?????????")) {
                    state.setText("?????????");
                    state.setTextColor(Color.GREEN);
                    isOpen = true;
                } else {
                    state.setText("?????????");
                    state.setTextColor(Color.RED);
                    isOpen = false;
                }

                String image = ParkingDetailedInfoActivity.this.parkingInfo.getMerchantimage();
                String[] path = image.split("&");
                List<String> paths = ToolUtil.arrayToList(path);

                banner.addBannerLifecycleObserver(ParkingDetailedInfoActivity.this)//???????????????????????????
                        .setIndicator(new CircleIndicator(ParkingDetailedInfoActivity.this))//???????????????
                        .setAdapter(new BannerImageAdapter<String>(paths) {
                            @Override
                            public void onBindView(BannerImageHolder holder, String data, int position, int size) {
                                Log.e("tag", RequestAPI.baseImageURL + data);
                                Glide.with(ParkingDetailedInfoActivity.this)
                                        .load(RequestAPI.baseImageURL + data)
                                        .into(holder.imageView);
                            }
                        });

                banner.setOnBannerListener(new OnBannerListener() {
                    @Override
                    public void OnBannerClick(Object data, int position) {
                        ShowPhotoDialog dialog = new ShowPhotoDialog(ParkingDetailedInfoActivity.this);
                        dialog.setPhoto(RequestAPI.baseImageURL + data);
                        dialog.show();
                    }
                });

                subscribenumber = parkingNumber.getAllnumber() - parkingNumber.getSubscribenumber() - parkingNumber.getUsednumber() - parkingNumber.getUnusednumber();

                merchantname.setText(ParkingDetailedInfoActivity.this.parkingInfo.getMerchantname());
                parking_address.setText(ParkingDetailedInfoActivity.this.parkingInfo.getMerchantaddress());
                if (nowLatlng != null)
                    distance.setText("??????" + ToolUtil.getDistance(ParkingDetailedInfoActivity.this.location, nowLatlng) + "??????");
                max_number.setText("???????????????:" + parkingNumber.getAllnumber() + "/???????????????:" + (subscribenumber > 0 ? subscribenumber : 0));
            }

            @Override
            protected void defeated(BaseModel<MerchantProperty> merchantPropertyBaseModel) {
                showToast(merchantPropertyBaseModel.getMessage());
            }

            @Override
            public void onError(ExceptionHandle.ResponeThrowable e) {
                Log.e("tag", e.getMessage());
            }
        });

        initComment();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initData();
    }
}
