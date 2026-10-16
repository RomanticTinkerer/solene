import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score, accuracy_score, f1_score
from sklearn.preprocessing import LabelEncoder
import os

def main():
    files = ['FedCycleData071012.csv', 'FedCycleData071012_deriv1.csv', 'FedCycleData071012_deriv2.csv']
    dfs = []
    for f in files:
        if os.path.exists(f):
            dfs.append(pd.read_csv(f))
            
    if not dfs:
        print("No data files found.")
        return

    df = pd.concat(dfs, ignore_index=True)

    # Replace spaces with NaN
    df = df.replace(' ', np.nan)
    df = df.replace('', np.nan)
    
    # We must convert CycleNumber to numeric to sort properly
    df['CycleNumber'] = pd.to_numeric(df['CycleNumber'], errors='coerce')

    # Sort by ClientID and then CycleNumber
    df = df.sort_values(by=['ClientID', 'CycleNumber'])

    targets = ['LengthofCycle', 'EstimatedDayofOvulation', 'CycleWithPeakorNot']
    
    # Create lagged targets (Next_...)
    next_targets = []
    for target in targets:
        if target in df.columns:
            next_col = f'Next_{target}'
            df[next_col] = df.groupby('ClientID')[target].shift(-1)
            next_targets.append(next_col)

    # Drop explicitly non-essential/text columns (keep ClientID for grouping, we'll drop it from X later)
    drop_cols = ['Medvitexplain', 'MedvitexplainM', 'Methoddate', 'Whychart', 'Religion', 'Ethnicity', 'OccupationM']
    df = df.drop(columns=[c for c in drop_cols if c in df.columns], errors='ignore')

    for next_target, original_target in zip(next_targets, targets):
        print(f"\n{'='*40}")
        print(f"--- Forecasting Target: {next_target} (based on previous cycle features) ---")
        
        # Drop rows where the next target is missing (e.g., the last cycle for each user)
        df_model = df.dropna(subset=[next_target]).copy()
        
        y = df_model[next_target]
        y = pd.to_numeric(y, errors='coerce')
        
        valid_idx = y.notna()
        y = y[valid_idx]
        df_model = df_model[valid_idx]
        
        # X excludes all 'Next_' targets to prevent leakage of future data
        # We also drop ClientID because it's just an identifier
        X = df_model.drop(columns=next_targets + ['ClientID'], errors='ignore')
        
        # Convert all object columns in X to numeric if possible, else label encode
        for col in X.columns:
            try:
                X[col] = pd.to_numeric(X[col])
            except (ValueError, TypeError):
                pass
            if not pd.api.types.is_numeric_dtype(X[col]):
                X[col] = X[col].astype(str)
                le = LabelEncoder()
                X[col] = le.fit_transform(X[col])
                
        # Train test split
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        print(f"Training data size: {X_train.shape[0]} samples, {X_train.shape[1]} features.")
        
        if original_target == 'CycleWithPeakorNot':
            model = xgb.XGBClassifier(eval_metric='logloss', random_state=42)
            model.fit(X_train, y_train)
            y_pred = model.predict(X_test)
            acc = accuracy_score(y_test, y_pred)
            f1 = f1_score(y_test, y_pred, average='weighted')
            print(f"Accuracy: {acc:.4f}")
            print(f"F1-Score: {f1:.4f}")
        else:
            model = xgb.XGBRegressor(random_state=42)
            model.fit(X_train, y_train)
            y_pred = model.predict(X_test)
            rmse = np.sqrt(mean_squared_error(y_test, y_pred))
            r2 = r2_score(y_test, y_pred)
            print(f"RMSE: {rmse:.4f}")
            print(f"R2 Score: {r2:.4f}")
            
        # Feature importance
        importances = model.feature_importances_
        indices = np.argsort(importances)[::-1]
        print("Top 5 important features (from the PREVIOUS cycle):")
        for i in range(min(5, len(indices))):
            print(f"  {i+1}. {X.columns[indices[i]]}: {importances[indices[i]]:.4f}")

if __name__ == "__main__":
    main()
