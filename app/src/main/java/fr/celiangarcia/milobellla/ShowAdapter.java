package fr.celiangarcia.milobellla;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ShowAdapter extends ArrayAdapter<Show> {
 
    //shows est la liste des models à afficher
    public ShowAdapter(Context context, List<Show> shows) {
        super(context, 0, shows);
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
 
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemview,parent, false);
        }
 
        TweetViewHolder viewHolder = (TweetViewHolder) convertView.getTag();
        if(viewHolder == null){
            viewHolder = new TweetViewHolder();
            viewHolder.pseudo = (TextView) convertView.findViewById(R.id.pseudo);
            viewHolder.text = (TextView) convertView.findViewById(R.id.text);
            viewHolder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
            convertView.setTag(viewHolder);
        }
 
        //getItem(position) va récupérer l'item [position] de la List<Show> tweets
        Show show = getItem(position);
 
        //il ne reste plus qu'à remplir notre vue
        viewHolder.pseudo.setText(show.getPseudo());
        viewHolder.text.setText(show.getText());
        viewHolder.avatar.setImageDrawable(new ColorDrawable(show.getColor()));
 
        return convertView;
    }
 
    private class TweetViewHolder{
        public TextView pseudo;
        public TextView text;
        public ImageView avatar;
    }
}