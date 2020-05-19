#include <jni.h>
#include "darknet.h"

jint JNICALL
Java_com_example_darknet_MainActivity_detectimg(JNIEnv* env, jobject obj) {
    char *cfgfile = "/sdcard/yolo/config/yolo.cfg";
    char *weightfile = "/sdcard/yolo/config/yolo.weights";
    char *listfile = "/sdcard/yolo/config/detect.lists";
    char *outputdir = "/sdcard/yolo/";
    float thresh = .25;
    float hier_thresh = .5;

    char *name_list = "sdcard/yolo/config/yolo.names";
    char **names = get_labels(name_list);
    char *name_info = "sdcard/yolo/config/class.info";
    char **infos = get_labels(name_info);

    image **alphabet = load_alphabet();
    network *net = load_network(cfgfile, weightfile, 0);
    set_batch_network(net, 1);
    int clnms = (net->layers[net->n-1]).classes;
    srand(2222222);
    char inbuff[256];
    char outbuff[256];
    char *input = inbuff;
    char *output = outbuff;
    float nms=.45;
    float *threshes = malloc(clnms* sizeof(float));
    for(int i = 0; i < clnms; ++i) threshes[i] = strtof(infos[i], NULL);
    FILE *listp;
    if((listp = fopen(listfile, "r")) == NULL) return 0;
    int flag = 0;
    while(getc(listp) != EOF){
        fseek(listp, -1, SEEK_CUR);
        fgets(input, 256, listp);
        strtok(input, "\n");

        image im = load_image_color(input,0,0);
        image sized = letterbox_image(im, net->w, net->h);

        float *X = sized.data;
        network_predict(net, X);
        int nboxes = 0;
        detection *dets = get_network_boxes(net, im.w, im.h, thresh, hier_thresh, 0, 1, &nboxes);

        if (nms) do_nms_sort(dets, nboxes, clnms, nms);
        int temp = draw_detections(im, dets, nboxes, names, threshes, alphabet, clnms);
        free_detections(dets, nboxes);

        if (temp) {
            flag++;
            for (int i = (int) strlen(input); i >= 0; i--)
                if (input[i] == '/') input[i] = '_';
                else if (input[i] == '.') input[i] = '\0';
            strcpy(output, outputdir);
            strcat(output, input);
            save_image(im, output);
        }
        free_image(im);
        free_image(sized);
    }
    fclose(listp);
    return flag;
}