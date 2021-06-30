package com.pervasive.airsimtracker;

public abstract class BaseTask<R> implements CustomCallable<R> {
    @Override
    public R call() throws Exception {
        return null;
    }

    @Override
    public void postExecute(R result) {
    }

    @Override
    public void preExecute() {
    }
}
