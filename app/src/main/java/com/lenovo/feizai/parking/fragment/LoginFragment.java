package com.lenovo.feizai.parking.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.customview.DialogCustomViewExtKt;
import com.lenovo.feizai.parking.R;
import com.lenovo.feizai.parking.customeractivity.CustomerMainActivity;
import com.lenovo.feizai.parking.base.BaseFragment;
import com.lenovo.feizai.parking.base.BaseModel;
import com.lenovo.feizai.parking.base.BaseObserver;
import com.lenovo.feizai.parking.entity.MessageEvent;
import com.lenovo.feizai.parking.entity.User;
import com.lenovo.feizai.parking.merchantactivity.MerchantMainActivity;
import com.lenovo.feizai.parking.net.ExceptionHandle;
import com.lenovo.feizai.parking.net.RetrofitClient;
import com.lenovo.feizai.parking.util.MD5Util;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTouch;
import me.leefeng.promptlibrary.PromptDialog;

/**
 * @author feizai
 * @date 01/11/2021 011 9:04:55 PM
 */
public class LoginFragment extends BaseFragment {

    @BindView(R.id.user)
    EditText user;
    @BindView(R.id.pass)
    EditText pass;
    @BindView(R.id.see)
    ImageView seeing;

    String role;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    protected void initView(View view) {
        Bundle bundle = getArguments();
        role = bundle.getString("role");
        List<String> dataset = new LinkedList<>(Arrays.asList("??????", "??????"));
    }

    @OnTouch(R.id.see)
    public void see(View arg0, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP://??????????????????????????????????????????
                pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                break;
            case MotionEvent.ACTION_DOWN://??????????????????????????????????????????
                pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                break;
            default:
                break;
        }
        //????????????EditText??????????????????
        CharSequence charSequence = pass.getText();
        if (charSequence instanceof Spannable) {
            Spannable spanText = (Spannable) charSequence;
            Selection.setSelection(spanText, charSequence.length());
        }
    }

    @OnClick(R.id.login)
    public void login() {
        PromptDialog promptDialog = new PromptDialog(getActivity());
        String username = user.getText().toString().trim();
        String password = pass.getText().toString().trim();
        if (username.length() < 3) {
            showToast("???????????????????????????3???");
            return;
        }
        if (password.length() < 6) {
            showToast("????????????????????????6???");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getContext(), "??????????????????!", Toast.LENGTH_SHORT).show();
        } else {
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "??????????????????!", Toast.LENGTH_SHORT).show();
            } else {
                User user = new User();
                user.setUsername(username);
                user.setPassword(MD5Util.string2MD5(password));
                user.setRole(role);
                RetrofitClient.getInstance(getContext())
                        .login(user, new BaseObserver<BaseModel>(getContext()) {
                            @Override
                            protected void showDialog() {
                                promptDialog.showLoading("????????????");
                            }

                            @Override
                            protected void hideDialog() {
                            }

                            @Override
                            protected void successful(BaseModel baseModel) {
                                SharedPreferences.Editor editor = getActivity().getSharedPreferences("userdata", Context.MODE_PRIVATE).edit();
                                editor.putString("username", username);
                                editor.putString("userpass", MD5Util.string2MD5(password));
                                editor.putString("role", role);
                                editor.apply();
                                promptDialog.showSuccess("????????????");
                                switch (role){
                                    case "??????":
                                        startActivity(CustomerMainActivity.class);
                                        getActivity().finish();
                                        break;
                                    case "??????":
                                        startActivity(MerchantMainActivity.class);
                                        getActivity().finish();
                                        break;
                                }
                            }

                            @Override
                            protected void defeated(BaseModel baseModel) {
                                promptDialog.showError("????????????");
                                Toast.makeText(getContext(), baseModel.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(ExceptionHandle.ResponeThrowable e) {
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                promptDialog.dismiss();
                            }
                        });
            }
        }
    }

    @OnClick(R.id.registertxt)
    public void registertxt() {
        EventBus.getDefault().postSticky(new MessageEvent("register"));
    }

    @OnClick(R.id.forgetpass)
    public void forgetpass() {
        EventBus.getDefault().postSticky(new MessageEvent("forget"));
    }

}
